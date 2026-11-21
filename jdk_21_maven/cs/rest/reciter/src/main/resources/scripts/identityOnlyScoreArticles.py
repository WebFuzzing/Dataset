import json
import sys		  
import pandas as pd
#import numpy as np
import os
os.environ['TF_CPP_MIN_LOG_LEVEL'] = '2'  # Suppress INFO, WARNING, and ERROR messages
#import logging
#logging.getLogger('absl').setLevel(logging.FATAL)
#import tensorflow as tf
#tf.get_logger().setLevel('FATAL')  # Only show fatal errors

import joblib
#import pymysql
import tensorflow as tf
from tensorflow.keras.models import load_model
import logging

import argparse
import boto3
from botocore.exceptions import NoCredentialsError
import warnings
warnings.filterwarnings('ignore')
try:
    from urllib3.exceptions import SNIMissingWarning
except ImportError:
    # Handle the absence or use an alternative
    SNIMissingWarning = None


tf.get_logger().setLevel('ERROR')  # Set TensorFlow logger to only show errors

# Ignore SNIMissingWarning
warnings.filterwarnings("ignore", category=UserWarning, message=".*SNI.*")
# Set up logging configuration
logging.basicConfig(filename='IdentityScore.log', level=logging.INFO, 
                    format='%(asctime)s - %(levelname)s - %(message)s')

logging.info("Current Working Directory: %s", os.getcwd())

# Set up argument parsing
parser = argparse.ArgumentParser(description="Read a JSON file.")
parser.add_argument('file_name', type=str, help='The name of the JSON file to read')
parser.add_argument('bucket_name', type=str, help='The name of the S3 bucket')
parser.add_argument('useS3Bucket', type=str, help='Flag whether to use S3 Bucket or not')

# Parse the arguments
args = parser.parse_args()
s3 = boto3.client('s3')







def upload_log_to_s3():
    try:
        s3 = boto3.client('s3')
        bucket_name = args.bucket_name
        log_file = 'IdentityScore.log'
        
        s3.upload_file(log_file, bucket_name, log_file)
        logging.info(f'Successfully uploaded {log_file} to {bucket_name}')
    except FileNotFoundError:
        logging.error(f'The file {log_file} was not found')
    except NoCredentialsError:
        logging.error('Credentials not available')
    except Exception as e:
        logging.error(f'Failed to upload {log_file} to {bucket_name}: {str(e)}')


def read_json_file(file_name):
    try:
        logging.info(f"Script is starting. {file_name}")
        with open(file_name, 'r') as file:
            data = json.load(file)
            return data  # Return the loaded JSON data
    except FileNotFoundError:
        logging.error(f"Error: The file '{file_name}' was not found.")
    except json.JSONDecodeError:
        logging.error("Error: The file is not a valid JSON.")
    except Exception as e:
        logging.error(f"Error reading JSON file: {e}")
        sys.exit(1)  # Exit if there's an error in loading the data
      

def read_file_from_s3(bucket_name, file_key):
    s3 = boto3.client('s3')

    try:
        # Get the object from S3
        response = s3.get_object(Bucket=bucket_name, Key=file_key)
        
        # Read the content of the file
        content = response['Body'].read().decode('utf-8')  # Decode bytes to string
        
        # If the file is JSON, load it into a Python dictionary
        data = json.loads(content)
        return data

    except Exception as e:
        logging.error(f"Error reading file from S3: {e}")
        return None
    
    finally:
        logging.info("ScriptIdentity finished")
        # Call the upload function with your S3 bucket name
        upload_log_to_s3()

def file_exists_in_s3(bucket_name, file_key):
    s3 = boto3.client('s3')
    
    try:
        # Try to retrieve metadata for the object
        s3.head_object(Bucket=bucket_name, Key=file_key)
        return True  # If no exception, the file exists
    except ClientError as e:
        # If a 404 error is raised, the file does not exist
        if e.response['Error']['Code'] == '404':
            return False
        else:
            # Raise other errors
            raise


# Fetch data and save to 'scoring_input.json'
#data = fetch_and_save_data()
logging.info('coming here')
# Check if the file exists before trying to read it
logging.info(f"The bucket flag '{args.useS3Bucket}' exists in the bucket '{args.bucket_name}'.")
if args.useS3Bucket == "false" and os.path.isfile(args.file_name):
    logging.info('reading the file from File folder:')
    data = read_json_file(args.file_name)
    if data is not None:
        logging.info(data)
elif args.useS3Bucket == "true" and file_exists_in_s3(args.bucket_name, args.file_name):
    logging.info(f"The file '{args.file_name}' exists in the bucket '{args.bucket_name}'.")
    # Proceed to read the file from S3
    data = read_file_from_s3(args.bucket_name, args.file_name)
    if data is not None:
        logging.info(data)
else:
    logging.info(f"Error: The file '{args.file_name}' does not exist locally and the file '{args.file_name}' does not exist in the bucket '{args.bucket_name}' and useS3Bucket is '{args.useS3Bucket}'.")
	




#data = read_json_file(args)
#json_data = sys.stdin.read()
#data = json.loads(json_data)
#logging.info(type(data))
#logging.info(f"data read from stdin: {data}")

# Load the pre-trained model and scaler
model = load_model('identityOnlyModel.keras')
scaler = joblib.load('identityOnlyScaler.save')

# Convert the list of dictionaries into a DataFrame
df = pd.DataFrame(data)

# Prepare features and labels from the data
df['label'] = df['userAssertion'].apply(lambda x: 1 if x == 'ACCEPTED' else 0)


# Prepare features and labels from the data
#data['label'] = data['userAssertion'].apply(lambda x: 1 if x == 'ACCEPTED' else 0)

# Features for prediction
feature_columns = [ 
    'articleCountScore','authorCountScore','discrepancyDegreeYearScore','emailMatchScore','genderScoreIdentityArticleDiscrepancy',
    'grantMatchScore','journalSubfieldScore','nameMatchFirstScore','nameMatchLastScore','nameMatchMiddleScore',
    'nameMatchModifierScore','organizationalUnitMatchingScore','scopusNonTargetAuthorInstitutionalAffiliationScore',
    'targetAuthorInstitutionalAffiliationMatchTypeScore','pubmedTargetAuthorInstitutionalAffiliationMatchTypeScore',
	'relationshipPositiveMatchScore', 'relationshipNegativeMatchScore', 'relationshipIdentityCount'
]
# Check which feature columns are actually present in the DataFrame
missing_columns = [col for col in feature_columns if col not in df.columns]
if missing_columns:
    logging.info(f"Warning: The following columns are missing: {missing_columns}")
    # Optionally, you can choose to exclude missing columns
    feature_columns = [col for col in feature_columns if col in df.columns]

X = df[feature_columns]

# Scale the features
features_scaled = scaler.transform(X)

# Run the predictions
predictions = model.predict(features_scaled, verbose=0)

# Prepare the output
scoring_output = []
for idx, row in df.iterrows():
    id_value = row['articleId']
    inverted_score = float(predictions[idx][0])
    scoring_output.append({'id': id_value, 'scoreTotal': inverted_score})


# Print the scoring output as JSON to return it to the Java process
logging.info(f"script execution completed successfully: {scoring_output}")
print(json.dumps(scoring_output))

# Save the inverted scores to a CSV file
#score_data = pd.DataFrame(scoring_output)
#score_data.to_csv('scoringOutput.csv', index=False)
#logging.info("Inverted scores saved to 'scoringOutput.csv'.")

# Save the scoring output to JSON
#with open('scoringOutput.json', 'w') as json_file:
 #   json.dump(scoring_output, json_file, indent=4)
#logging.info("Scoring results saved to 'scoringOutput.json'.")

