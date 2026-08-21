package jasper.component;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.exif.ExifIFD0Directory;
import io.micrometer.core.annotation.Timed;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

@Component
public class Images {
	private static final Logger logger = LoggerFactory.getLogger(Images.class);
	private static final int THUMBNAIL_SIZE = 192;

	@Timed(value = "jasper.images")
	public byte[] thumbnail(InputStream image) {
		try (image) {
			return thumbnail(image.readAllBytes());
		} catch (IOException e) {
			return null;
		}
	}

	@Timed(value = "jasper.images")
	public byte[] thumbnail(byte[] imageData) {
		try {
			// Read orientation from metadata
			var orientation = 1;
			try {
				var metadata = ImageMetadataReader.readMetadata(new ByteArrayInputStream(imageData));
				var directory = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
				if (directory != null && directory.containsTag(ExifIFD0Directory.TAG_ORIENTATION)) {
					orientation = directory.getInt(ExifIFD0Directory.TAG_ORIENTATION);
				}
			} catch (Exception e) {
				logger.debug("Error reading EXIF data", e);
			}

			// Read and process the image
			var bi = ImageIO.read(new ByteArrayInputStream(imageData));
			if (bi == null) {
				logger.debug("Could not read image");
				return null;
			}

			// Apply orientation if needed
			bi = rotateImageIfNeeded(bi, orientation);

			var bo = new ByteArrayOutputStream();
			var width = bi.getWidth();
			var height = bi.getHeight();

			if (width > THUMBNAIL_SIZE || height > THUMBNAIL_SIZE) {
				var ar = (double) width / height;
				if (width > height) {
					width = THUMBNAIL_SIZE;
					height = (int) Math.floor(THUMBNAIL_SIZE / ar);
				} else {
					height = THUMBNAIL_SIZE;
					width = (int) Math.floor(THUMBNAIL_SIZE * ar);
				}
				var scaled = bi.getScaledInstance(width, height, Image.SCALE_SMOOTH);
				ImageIO.write(buffer(scaled, width, height), "png", bo);
			} else {
				return null;
			}

			return bo.toByteArray();
		} catch (Exception e) {
			logger.debug("Error resizing thumbnail", e);
			return null;
		}
	}

	private BufferedImage rotateImageIfNeeded(BufferedImage image, int orientation) {
		return switch (orientation) {
			case 1 -> image; // Normal
			case 3 -> // 180 rotate
				rotate(image, Math.PI);
			case 6 -> // 90 CW
				rotate(image, Math.PI / 2);
			case 8 -> // 270 CW
				rotate(image, -Math.PI / 2);
			default -> image;
		};
	}

	private BufferedImage rotate(BufferedImage image, double angle) {
		var w = image.getWidth();
		var h = image.getHeight();

		var rotated = new BufferedImage(w, h, image.getType());
		var g = rotated.createGraphics();

		g.rotate(angle, w/2, h/2);
		g.drawImage(image, 0, 0, null);
		g.dispose();

		return rotated;
	}

	public static BufferedImage buffer(Image image, int width, int height) {
		if (image instanceof BufferedImage) return (BufferedImage) image;
		var bi = new BufferedImage(width, height, BufferedImage.TYPE_INT_ARGB);
		var g = bi.createGraphics();
		g.drawImage(image, 0, 0, null);
		g.dispose();
		return bi;
	}
}
