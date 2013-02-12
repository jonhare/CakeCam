To set up for development with eclipse type:

mvn eclipse:eclipse


Then when you import it in Eclipse, set up your M2_REPO variable to point to 
- Mac/Unix: ~/.m2/repository
- Windows:  C:\Users\<username>\.m2\repository


Run mvn assembly:assembly in the folder to set things up and create target/

Add target/boardcam-1.0-SNAPSHOT-jar-with-dependencies.jar to build path.



## Dependecies (not yet handled by MVN)
twitter4j-core-2.2.6.jar


## Configuring CakeCam for your institution

Edit the cakecam.properties file.
Edit the twitter4j.properties file to post to your Twitter account.