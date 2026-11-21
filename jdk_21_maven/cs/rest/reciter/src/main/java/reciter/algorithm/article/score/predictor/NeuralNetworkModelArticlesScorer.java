package reciter.algorithm.article.score.predictor;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import org.json.JSONArray;
import org.json.JSONObject;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StopWatch;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import reciter.storage.s3.AwsScoringClients;
import reciter.utils.PropertiesUtils;
import software.amazon.awssdk.core.SdkBytes;
import software.amazon.awssdk.services.lambda.LambdaClient;
import software.amazon.awssdk.services.lambda.model.InvokeRequest;
import software.amazon.awssdk.services.lambda.model.InvokeResponse;

public class NeuralNetworkModelArticlesScorer {

	private static final Logger log = LoggerFactory.getLogger(NeuralNetworkModelArticlesScorer.class);

	private static final String LAMBDA_NAME = "RECITER_SCORING_LAMBDA_FUNCTION";

	private static final String LAMBDA_FUNCTION_INVOCATION_URL = "local.lambda.function.invocation.url";

	private static final String LAMBDA_FUNCTION_REGION = "aws.lambda.region";

	private String reciterScoringServiceUrl = System.getenv("RECITERSCORING_SERVICE_URL");

	public NeuralNetworkModelArticlesScorer() {
	}

	public JSONArray executeArticleScorePredictor(String goldStandardName, String dataFileName, String s3BucketName,
			String isS3UploadRequiredString) throws JsonMappingException, JsonProcessingException {

		StopWatch stopWatch = new StopWatch(goldStandardName);
		stopWatch.start(goldStandardName);
		JSONArray authorshipLikelihoodScore;

		if (isS3UploadRequiredString != null && !isS3UploadRequiredString.equalsIgnoreCase("")
				&& (isS3UploadRequiredString == "false" || isS3UploadRequiredString.equalsIgnoreCase("false"))) {
			authorshipLikelihoodScore = callLocalLambda(goldStandardName, dataFileName, s3BucketName,
					isS3UploadRequiredString);
		} else {
			String lambdaFunction = System.getenv(LAMBDA_NAME);
			log.info("lambdaFunction Name:" + lambdaFunction);

			authorshipLikelihoodScore = callAwsLambda(goldStandardName, dataFileName, s3BucketName,
					isS3UploadRequiredString, lambdaFunction);
		}

		stopWatch.stop();
		log.info(stopWatch.getId() + " took " + stopWatch.getTotalTimeSeconds() + "s");
		return authorshipLikelihoodScore;
	}

	// Helper method to find JSONObject by article
	private static void findJSONObjectById(JSONArray jsonArray, long articleId) {
		for (int i = 0; i < jsonArray.length(); i++) {
			JSONObject jsonObject = jsonArray.getJSONObject(i);
			if (jsonObject.getLong("id") == articleId) {
				double score = jsonObject.getDouble("scoreTotal") * 100;
			}
		}
	}

	public static void main(String args[]) throws JsonMappingException, JsonProcessingException {
		NeuralNetworkModelArticlesScorer nn = new NeuralNetworkModelArticlesScorer();
		JSONArray articlesIdentityFeedbackScoreTotal = nn.executeArticleScorePredictor("feedback",
				"ajg9004-feedbackIdentityScoringInput.json", "feedbackScore", "false");
		if (articlesIdentityFeedbackScoreTotal != null && articlesIdentityFeedbackScoreTotal.length() > 0)
			findJSONObjectById(articlesIdentityFeedbackScoreTotal, 9856924);

	}

	@SuppressWarnings("unused")

	private JSONArray callLocalLambda(String goldStandardModelName, String articleDataFilename, String s3BucketName,
			String isS3UploadRequiredString) {
		URL url = null;
		HttpURLConnection conn = null;
		try {
			url = new URL(reciterScoringServiceUrl + PropertiesUtils.get(LAMBDA_FUNCTION_INVOCATION_URL));
			conn = (HttpURLConnection) url.openConnection();
			if (conn != null) {
				conn.setRequestMethod("POST");
				conn.setRequestProperty("Content-Type", "application/json");
				conn.setDoOutput(true);
			}
		} catch (Exception e) {
			log.error("Failed to open Lambda HTTP connection to {} for model={}, dataFile={}", reciterScoringServiceUrl,
					goldStandardModelName, articleDataFilename, e);
		}

		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("modelName", goldStandardModelName);
		payloadMap.put("scoringDataFile", articleDataFilename);
		payloadMap.put("useS3Bucket", isS3UploadRequiredString);
		payloadMap.put("bucket_name", s3BucketName);

		String payload = null;
		try {
			payload = mapper.writeValueAsString(payloadMap);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize local-Lambda scoring payload for model={}, dataFile={}",
					goldStandardModelName, articleDataFilename, e);
		}

		try {
			if (conn != null) {
				// Write payload to Lambda container
				try (OutputStream os = conn.getOutputStream()) {
					os.write(payload.getBytes(StandardCharsets.UTF_8));
				}
				// Read response from Lambda container

				// Now it's safe to read the response
				int responseCode = conn.getResponseCode();
				// Read response from Lambda
				StringBuilder response = new StringBuilder();

				try (BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()))) {
					String output;
					while ((output = reader.readLine()) != null) {
						response.append(output);

					}
				}
				// Parse the response
				JSONArray predictionScoresArray = null;
				JSONObject outer = new JSONObject(response.toString());
				if (outer.get("predictionScores") instanceof JSONArray) {
					predictionScoresArray = outer.getJSONArray("predictionScores");
				}
				log.info(" predictionScoresArray " + predictionScoresArray);

				// return Code
				int returnCode = outer.getInt("returnCode");
				log.info("returnCode: " + returnCode);

				if (returnCode == 0) {
					return predictionScoresArray;
				} else

				{
					// Log the error from the Python scoring script
					String error = outer.optString("error");
					log.error("Lambda scoring failed with returncode {}. error: {}", returnCode, error);
					return null;

				}
			}

		} catch (IOException | RuntimeException e) {
			log.error("Local Lambda scoring invocation/parse failed for model={}, dataFile={}; returning null",
					goldStandardModelName, articleDataFilename, e);
		}
		return null;
	}

	/*
	 * Calls AWS Lambda function
	 */
	private JSONArray callAwsLambda(String goldStandardModelName, String articleDataFilename, String s3BucketName,
			String isS3UploadRequiredString, String lambdaFunctionName) {

		LambdaClient client = AwsScoringClients.lambda(PropertiesUtils.get(LAMBDA_FUNCTION_REGION));

		ObjectMapper mapper = new ObjectMapper();

		Map<String, Object> payloadMap = new HashMap<>();
		payloadMap.put("modelName", goldStandardModelName);
		payloadMap.put("scoringDataFile", articleDataFilename);
		payloadMap.put("useS3Bucket", isS3UploadRequiredString);
		payloadMap.put("bucket_name", s3BucketName);

		String payloadJson = null;
		try {
			payloadJson = mapper.writeValueAsString(payloadMap);
		} catch (JsonProcessingException e) {
			log.error("Failed to serialize AWS Lambda scoring payload for model={}, dataFile={}", goldStandardModelName,
					articleDataFilename, e);
		}

		// v2: InvokeRequest.builder() replaces new InvokeRequest().withFunctionName()
		// v2: SdkBytes.fromUtf8String() replaces plain String payload
		InvokeRequest request = InvokeRequest.builder().functionName(lambdaFunctionName)
				.payload(SdkBytes.fromUtf8String(payloadJson)).build();

		try {
			// v2: LambdaClient.invoke() replaces AWSLambda.invoke()
			// v2: InvokeResponse replaces InvokeResult
			InvokeResponse result = client.invoke(request);

			// v2: result.payload().asUtf8String() replaces result.getPayload().array()
			String response = result.payload().asUtf8String();

			JSONObject outer = new JSONObject(response);
			JSONArray predictionScoresArray = null;
			if (outer.get("predictionScores") instanceof JSONArray) {
				predictionScoresArray = outer.getJSONArray("predictionScores");
			}
			log.info(" predictionScoresArray " + predictionScoresArray);

			// return Code
			int returnCode = outer.getInt("returnCode");
			log.info("returnCode: " + returnCode);

			if (returnCode == 0) {
				return predictionScoresArray;

			} else {

				// Log the error from the Python scoring script
				String error = outer.optString("error");
				log.error("Lambda scoring failed with returncode {}. error: {}", returnCode, error);
				return null;

			}

		} catch (Exception e) {
			log.error("AWS Lambda scoring invocation/parse failed for function={}, model={}, dataFile={}",
					lambdaFunctionName, goldStandardModelName, articleDataFilename, e);
		}
		return null;
	}

}