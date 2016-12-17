<h1>Deployment from the CLI</h1>
Issue the following command from the repo's root to both package the JAR and update a Lambda function. This assumes that you have your AWS credentials file straight, and that you have modified this command to update the right Lambda function.<br>
<code>mvn assembly:assembly -DdescriptorId=jar-with-dependencies package; aws --profile dixonaws@amazon.com lambda update-function-code --function-name Hello-World-Example-Skill --zip-file fileb:////Users//dixonaws//Developer//ideaWorkspace//alexa-skills-kit-java//samples//target//alexa-skills-kit-samples-1.0-jar-with-dependencies.jar</code>
 
