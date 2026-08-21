package jasper.service.dto;

import lombok.Getter;
import lombok.Setter;

import java.io.Serializable;

@Getter
@Setter
public class SmtpWebhookDto implements Serializable {
	private String spf;
	private String[] references;

	private String id;
	private String date;
	private String subject;

	private String resentDate;
	private String resentId;

	private Body body;
	private Addresses addresses;

	private EmailAttachment[] attachments;
	private EmailEmbeddedFile[] embeddedFiles;

	@Getter
	@Setter
	public static class Body {
		private String text;
		private String html;
	}

	@Getter
	@Setter
	public static class Addresses {
		private EmailAddress from;
		private EmailAddress to;
		private EmailAddress[] replyTo;
		private EmailAddress[] cc;
		private EmailAddress[] bcc;
		private String[] inReplyTo;
		private String html;
		private EmailAddress resentFrom;
		private EmailAddress[] resentTo;
		private EmailAddress[] resentCc;
		private EmailAddress[] resentBcc;
	}

	@Getter
	@Setter
	public static class EmailAddress {
		private String name;
		private String address;
	}

	@Getter
	@Setter
	public static class EmailAttachment {
		private String filename;
		private String contentType;
		private String data;
	}

	@Getter
	@Setter
	public static class EmailEmbeddedFile {
		private String cid;
		private String contentType;
		private String data;
	}
}

