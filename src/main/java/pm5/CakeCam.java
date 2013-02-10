package pm5;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.HashSet;
import java.util.Properties;
import javax.activation.DataHandler;
import javax.activation.DataSource;
import javax.activation.FileDataSource;
import javax.mail.BodyPart;
import javax.mail.Message;
import javax.mail.MessagingException;
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
import org.openimaj.image.colour.ColourSpace;
import org.openimaj.video.VideoDisplay;
import org.openimaj.video.VideoDisplayListener;
import org.openimaj.video.capture.VideoCapture;

import twitter4j.*;

import javax.imageio.stream.FileImageInputStream;
import javax.imageio.ImageIO;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.NotFoundException;
import com.google.zxing.multi.qrcode.QRCodeMultiReader;
import com.google.zxing.Result;
/**
 * TODO
 * [ ] Store image in memory, rather than temporary file.
 * [ ] Some solution to not send e-mail when there is no food.
 * [x] Load mail host from properties file
 * [x] Load mail addresses from properties file
 * [x] Load food location from properties file
 * [x] Load food names from properties file
 */
public class CakeCam 
{
    public static void main(String[] args) 
    {
        //Testing on windows
        final String PATH = System.getProperty("java.io.tmpdir")+"CakeCam/";
        //final String emailFromAddress = "cakecam@ecs.soton.ac.uk";
        //final String emailToAddress = "rfp@ecs.soton.ac.uk";
        
        // Load cakecam.properties
        final Properties ccProperties = new Properties();
        try
		{
			ccProperties.load(new FileInputStream("cakecam.properties"));
		}
		catch (FileNotFoundException fnfe)
		{
			System.err.println("Could not find cakecam.properties. Message: "+fnfe.getMessage());
		}
		catch (IOException ioe)
		{
			System.err.println("Failed to load cakecam.properties. Message: "+ioe.getMessage());
		}
        
        VideoCapture vc = null;
        try 
        {
            vc = new VideoCapture(640,480);
        }
        catch (IOException e)
        {
            System.err.println("IO error: "+e.getMessage());
            //e.printStackTrace();
        }
        
        VideoDisplay<MBFImage> vd = VideoDisplay.createOffscreenVideoDisplay(vc);
        
        VideoDisplayListener<MBFImage> listener = new VideoDisplayListener<MBFImage>() 
        {
            String fileName = PATH+"cake.jpg";
            int frameCount = 0;
            
            @Override
            public void beforeUpdate(MBFImage frame)
            {
                frame = ColourSpace.RGB.convertFromRGB(frame);
                DisplayUtilities.displayName(frame, "CakeCam");
                
                // Delay to allow user to check photo?
                if(frameCount<200)
                {
                    frameCount++;
                    return;
                }
                
                // Write photo to file
                File file = new File(fileName);
                try
                {
	                if(!file.exists())
	                {
	            		if(!file.mkdirs())
	            			System.err.println("Failed to make required directories.");
						file.createNewFile();
	                }
                    ImageIO.write(ImageUtilities.createBufferedImageForDisplay(frame), "jpg", file);
                }
                catch (IOException e)
                {
                    System.err.println("IO error: "+e.getMessage());
                    //e.printStackTrace();
                }
                
                //Define allowed QR codes
                HashSet<String> FOOD = new HashSet<String>();
                for(String F : ccProperties.getProperty("cakecam.FOOD").split(","))
                {
                	FOOD.add(F.trim());
                }
                /*BufferedReader filereader;
                try {
                    filereader = new BufferedReader(new FileReader("cakecamcodes.txt"));
                
                    while(filereader.ready())
                    {
                        String line = filereader.readLine().toLowerCase();
                        if(!line.startsWith("#"))
                        {
                            FOOD.add(line);
                        }
                    }
                }
                catch (FileNotFoundException e2) 
                {
                    System.err.println("File not found: "+e2.getMessage());
                    //e2.printStackTrace();
                }
                catch (IOException e)
                {
                    System.err.println("IO error: "+e.getMessage());
                    //e.printStackTrace();
                }*/

                //Create default subject
                String food = "cake (or other food)";
                
                //Convert File to zxing readable format
                BufferedImageLuminanceSource lumSource;
                try 
                {
                    lumSource = new BufferedImageLuminanceSource(ImageIO.read(new FileImageInputStream(file)));
                    BinaryBitmap image = new BinaryBitmap(new HybridBinarizer(lumSource));
                    
                    //Create QR code reader, decode available barcodes
                    QRCodeMultiReader reader = new QRCodeMultiReader();
                    Result[] results;

                    results = reader.decodeMultiple(image);
                    
                    int count = 0;
                    food = "";
                    boolean accepted = false;
                    
                    for(Result result : results)
                    {
                        if(FOOD.contains(result.getText().toLowerCase()))
                        {
                            food += result.getText().toLowerCase();
                            accepted = true;
                        }
                        
                        if(count == results.length - 2)
                        {
                            food += " and ";
                        }
                        else if(count < results.length -1)
                        {
                            food += ", ";
                        }
                        
                        count++;
                    }
                    //Revert to default subject if no acceptable codes were found
                    if(!accepted)
                    {
                        food = "cake (or other food)";
                    }
                }
                catch (FileNotFoundException e1)
                {
                    System.err.println("File not found: "+e1.getMessage());
                    //e1.printStackTrace();
                }
                catch (IOException e1)
                {
                    System.err.println("IO error: "+e1.getMessage());
                    //e1.printStackTrace();
                }
                catch (NotFoundException e1)
                {
                    System.err.println("No QR codes found: "+e1.getMessage());
                    //e1.printStackTrace();
                }
                
                String location = ccProperties.getProperty("cakecam.location", "regular food location");

                Properties properties = System.getProperties();

                // Setup mail server
                properties.setProperty("mail.smtp.host", ccProperties.getProperty("mail.smtp.host"));

                // Get the default Session object.
                Session session = Session.getDefaultInstance(properties);

                try
                {
                    MimeMessage message = new MimeMessage(session);

                    message.setFrom(new InternetAddress(ccProperties.getProperty("mail.from.address"), ccProperties.getProperty("mail.from.name")));
                     
                    message.addRecipient(Message.RecipientType.TO, new InternetAddress(ccProperties.getProperty("mail.to.address")));

                    message.setSubject("[CakeCam] - "+food+" in "+location);

                    BodyPart messageBodyPart = new MimeBodyPart();

                    // Fill the message
                    messageBodyPart.setText("This "+food+" can be found in the "+location+".");

                    Multipart multipart = new MimeMultipart();
                    multipart.addBodyPart(messageBodyPart);

                    // Part two is attachment
                    messageBodyPart = new MimeBodyPart();
                    DataSource source = new FileDataSource(fileName);
                    messageBodyPart.setDataHandler(new DataHandler(source));
                    messageBodyPart.setFileName(fileName);
                    multipart.addBodyPart(messageBodyPart);

                    // Put parts in message
                    message.setContent(multipart);


                    Transport.send(message);
                    System.out.println("Sent message successfully....");
                    
                    postToTwitter();
            		Twitter twitter = new TwitterFactory().getInstance();
            		twitter.updateProfileImage(file);
            		StatusUpdate status = new StatusUpdate("This "+food+" can be found in the "+location+"!");
            		status.setMedia(file);
            		twitter.updateStatus(status);
            		
            		System.out.println("Tweet posted successfully....");
                }
                catch (MessagingException mex)
                {
                     mex.printStackTrace();
                }
                catch (UnsupportedEncodingException e)
                {
                    System.err.println("Encoding error: "+e.getMessage());
                    e.printStackTrace();
                }
				catch (TwitterException e)
				{
					e.printStackTrace();
				}
                
                System.exit(0);
            }
            
            @Override
            public void afterUpdate(VideoDisplay<MBFImage> display)
            {
                // TODO Auto-generated method stub
            }
        };
        vd.addVideoListener(listener);
    }

	protected static void postToTwitter()
	{
	}
}
