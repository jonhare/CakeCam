## Building with Maven
Run ```mvn assembly:assembly``` in the folder, which will set things up and create the ```target/``` directory.

You can then run CakeCam with:
	
	java -jar  target/CakeCam-1.0-SNAPSHOT-jar-with-dependencies.jar 

##Developing in Eclipse
To set up for development with eclipse type:

    mvn eclipse:eclipse

Then when you import it in Eclipse (if you've not used Maven and Eclipse together before), set up your M2_REPO variable to point to 
- Mac/Unix: ~/.m2/repository
- Windows:  C:\Users\<username>\.m2\repository


## Configuring CakeCam for your institution

Edit the cakecam.properties file.

Edit the twitter4j.properties file to post to your Twitter account.
