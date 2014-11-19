package pm5;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.util.HashSet;
import java.util.Properties;

import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.Multipart;
import javax.mail.Session;
import javax.mail.Transport;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeBodyPart;
import javax.mail.internet.MimeMessage;
import javax.mail.internet.MimeMultipart;

import org.openimaj.image.DisplayUtilities;
import org.openimaj.image.ImageUtilities;
import org.openimaj.image.MBFImage;
import org.openimaj.video.capture.VideoCapture;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;

public class CakeCam
{
	private static final String DEFAULT_FOOD = "cake (or other food)";

	public static void main(String[] args)
	{
		VideoCapture vc = null;
		try
		{
			vc = new VideoCapture(640, 480);
		} catch (final IOException e)
		{
			System.err.println("Fatal camera error: " + e.getMessage());
			System.exit(1);
		}

		for (int frameCount = 0; frameCount < 200; frameCount++) {
			DisplayUtilities.displayName(vc.getNextFrame(), "CakeCam");
			try {
				Thread.sleep(1000 / 25);
			} catch (final InterruptedException e) {
				// ignore
			}
		}

		final MBFImage frame = vc.getNextFrame();
		File file = null;
		try
		{
			file = File.createTempFile("cake", ".jpg");
			ImageUtilities.write(frame, file);
		} catch (final IOException e) {
			System.err.println("Fatal IO error: " + e.getMessage());
			System.exit(1);
		}

		// Load cakecam.properties
		final Properties ccProperties = loadSettings();

		// Define allowed QR codes
		final HashSet<String> foods = readFoods(ccProperties);

		final String food = getFood(frame, foods);
		final String location = ccProperties.getProperty("cakecam.location", "regular food location");

		sendMail(ccProperties, file, food, location);
		postToTwitter(file, food, location);

		System.exit(0);
	}

	private static HashSet<String> readFoods(final Properties ccProperties) {
		final HashSet<String> foods = new HashSet<String>();
		for (final String F : ccProperties.getProperty("cakecam.FOOD").split(",")) {
			foods.add(F.trim());
		}
		return foods;
	}

	private static Properties loadSettings() {
		final Properties ccProperties = new Properties();
		try
		{
			ccProperties.load(new FileInputStream("cakecam.properties"));
		} catch (final IOException ioe)
		{
			System.err.println("Failed to load cakecam.properties. Message: " + ioe.getMessage());
			System.exit(1);
		}
		return ccProperties;
	}

	private static void
			sendMail(final Properties ccProperties, final File file, final String food, final String location)
	{
		final Properties properties = System.getProperties();

		// Setup mail server
		properties.setProperty("mail.smtp.host", ccProperties.getProperty("mail.smtp.host"));

		// Get the default Session object.
		final Session session = Session.getDefaultInstance(properties);

		try
		{
			final MimeMessage message = new MimeMessage(session);

			message.setFrom(new InternetAddress(ccProperties.getProperty("mail.from.address"), ccProperties
					.getProperty("mail.from.name")));

			message.addRecipient(Message.RecipientType.TO,
					new InternetAddress(ccProperties.getProperty("mail.to.address")));

			message.setSubject("[CakeCam] - " + food + " in " + location);

			BodyPart messageBodyPart = new MimeBodyPart();

			// Fill the message
			messageBodyPart.setText("This " + food + " can be found in the " + location + ".");

			final Multipart multipart = new MimeMultipart();
			multipart.addBodyPart(messageBodyPart);

			// Part two is attachment
			messageBodyPart = new MimeBodyPart();
			final DataSource source = new FileDataSource(file);
			messageBodyPart.setDataHandler(new DataHandler(source));
			messageBodyPart.setFileName(file.getName());
			multipart.addBodyPart(messageBodyPart);

			// Put parts in message
			message.setContent(multipart);

			Transport.send(message);
			System.out.println("Sent message successfully....");
		} catch (final Exception e) {
			System.err.println("ERROR: " + e.getMessage());
			e.printStackTrace();
		}
	}

	private static String getFood(final MBFImage frame, final HashSet<String> foods) {
		// Create default subject
		String food = DEFAULT_FOOD;

		try
		{
			// Convert File to zxing readable format
			final BufferedImageLuminanceSource lumSource = new BufferedImageLuminanceSource(
					ImageUtilities.createBufferedImage(frame));
			final BinaryBitmap image = new BinaryBitmap(new HybridBinarizer(lumSource));

			// Create QR code reader, decode available barcodes
			final QRCodeMultiReader reader = new QRCodeMultiReader();
			Result[] results;

			results = reader.decodeMultiple(image);

			int count = 0;
			food = "";
			boolean accepted = false;

			for (final Result result : results) {
				if (foods.contains(result.getText().toLowerCase())) {
					food += result.getText().toLowerCase();
					accepted = true;
				}

				if (count == results.length - 2) {
					food += " and ";
				} else if (count < results.length - 1) {
					food += ", ";
				}

				count++;
			}

			// Revert to default subject if no acceptable codes were
			// found
			if (!accepted)
				return DEFAULT_FOOD;
			return food;
		} catch (final NotFoundException e1) {
			System.err.println("INFO: No QR codes found, using default food string");
		}

		return DEFAULT_FOOD;
	}

	protected static void postToTwitter(File file, String food, String location)
	{
		// Twitter twitter = new TwitterFactory().getInstance();
		// twitter.updateProfileImage(file);
		// StatusUpdate status = new
		// StatusUpdate("This "+food+" can be found in the "+location+"!");
		// status.setMedia(file);
		// twitter.updateStatus(status);
		// System.out.println("Tweet posted successfully....");
	}
}
