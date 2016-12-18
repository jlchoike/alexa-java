/**
    Copyright 2014-2015 Amazon.com, Inc. or its affiliates. All Rights Reserved.

    Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance with the License. A copy of the License is located at

        http://aws.amazon.com/apache2.0/

    or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions and limitations under the License.
 */
package fleetbriefing;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.amazon.speech.slu.Intent;
import com.amazon.speech.speechlet.IntentRequest;
import com.amazon.speech.speechlet.LaunchRequest;
import com.amazon.speech.speechlet.Session;
import com.amazon.speech.speechlet.SessionEndedRequest;
import com.amazon.speech.speechlet.SessionStartedRequest;
import com.amazon.speech.speechlet.Speechlet;
import com.amazon.speech.speechlet.SpeechletException;
import com.amazon.speech.speechlet.SpeechletResponse;
import com.amazon.speech.ui.PlainTextOutputSpeech;
import com.amazon.speech.ui.Reprompt;
import com.amazon.speech.ui.SimpleCard;

import java.io.*;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.Statement;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
//import com.amazonaws.athena.jdbc.AthenaDriver;

/**
 * This sample shows how to create a simple speechlet for handling speechlet requests.
 */
public class FleetBriefingSpeechlet implements Speechlet {
    private static final Logger log = LoggerFactory.getLogger(FleetBriefingSpeechlet.class);

    @Override
    public void onSessionStarted(final SessionStartedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionStarted requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any initialization logic goes here
    }

    @Override
    public SpeechletResponse onLaunch(final LaunchRequest request, final Session session)
            throws SpeechletException {
        log.info("onLaunch requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        return getWelcomeResponse();
    }

    @Override
    public SpeechletResponse onIntent(final IntentRequest request, final Session session)
            throws SpeechletException {
        log.info("onIntent requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());

        Intent intent = request.getIntent();
        String intentName = (intent != null) ? intent.getName() : null;

        if ("HelloWorldIntent".equals(intentName)) {
            return getHelloResponse();
        } else if("FleetBriefingIntent".equals(intentName)) {
            return getHelloResponse();
        }
        else if ("LocationIntent".equals(intentName)) {
                return getLocationResponse();
            }
         else if ("AMAZON.HelpIntent".equals(intentName)) {
            return getHelpResponse();
        } else {
            throw new SpeechletException("Invalid Intent");
        }
    }

    @Override
    public void onSessionEnded(final SessionEndedRequest request, final Session session)
            throws SpeechletException {
        log.info("onSessionEnded requestId={}, sessionId={}", request.getRequestId(),
                session.getSessionId());
        // any cleanup logic goes here
    }

    /**
     * Creates and returns a {@code SpeechletResponse} with a welcome message.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getWelcomeResponse() {
        Date today=new Date();
        SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("EEEEMMMMdd");
        String dateString = DATE_FORMAT.format(today);

        String speechText = "";
        speechText+="Here's your fleet briefing for today, " + dateString + ",";

        speechText+=getFleetData();
        System.out.println(speechText);

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("FleetBriefing for " + dateString);
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        //return SpeechletResponse.newTellResponse(speech, card);
        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }

    private String getAthenaFleetData() {
        String speechText="";
        File fout = new File("/tmp/athenaCredentials");
        FileOutputStream fos = null;

        String athenaUrl = "jdbc:awsathena://athena.us-east-1.amazonaws.com:443";

        try {
            fout.getParentFile().createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fos = new FileOutputStream(fout);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        OutputStreamWriter osw = new OutputStreamWriter(fos);

        try {
            osw.write("region=us-east-1\n" +
                    "accessKey=AKIAIBRVKXTVSKR6D7XA\n" +
                    "secretKey=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\n");

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Connection conn = null;
        Statement statement = null;

        try {
            Class Athena = com.amazonaws.athena.jdbc.AthenaDriver.class;

            Class.forName("com.amazonaws.athena.jdbc.AthenaDriver");
            Properties info = new Properties();
            info.put("s3_staging_dir", "s3://dixonaws-athena-result-bucket/test/");
            //info.put("log_path", "/Users/myUser/.athena/athenajdbc.log");
            info.put("aws_credentials_provider_class","com.amazonaws.auth.PropertiesFileCredentialsProvider");
            info.put("aws_credentials_provider_arguments","/tmp/athenaCredentials");

            String databaseName = "fleetbriefing";

            System.out.println("Connecting to Athena...");
            conn = DriverManager.getConnection(athenaUrl, info);

            System.out.println("Listing tables in " + databaseName + "...");
            String sql = "SELECT pickup_location, count(rental_date) AS Rentals, sum(charges) AS Revenue FROM fleetbriefing.invoices_gzip GROUP BY  pickup_location ORDER BY  Rentals DESC LIMIT 3";
            statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                //Retrieve rental info
                int pickup_location=1;
                int rentals=2;
                int revenue=3;

                String str_pickup_location = rs.getString(pickup_location);

                //Accumulate values
                speechText="Pickup location: " + str_pickup_location + ", with " + rs.getString(rentals) + " rentals and " + rs.getString(revenue) + " in revenue";
            }
            rs.close();
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (statement != null)
                    statement.close();
            } catch (Exception ex) {

            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }

        return(speechText);
    }

    private String getFleetData() {
        String speechText="";
        File fout = new File("/tmp/athenaCredentials");
        FileOutputStream fos = null;

        String athenaUrl = "jdbc:awsathena://athena.us-east-1.amazonaws.com:443";

        /*
        try {
            fout.getParentFile().createNewFile();
        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            fos = new FileOutputStream(fout);
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }

        OutputStreamWriter osw = new OutputStreamWriter(fos);

        try {
            osw.write("region=us-east-1\n" +
                    "accessKey=accesskey\n" +
                    "secretKey=secretkey\n");

        } catch (IOException e) {
            e.printStackTrace();
        }

        try {
            osw.close();
        } catch (IOException e) {
            e.printStackTrace();
        }


        Connection conn = null;
        Statement statement = null;

        try {
            Class Athena = com.amazonaws.athena.jdbc.AthenaDriver.class;

            Class.forName("com.amazonaws.athena.jdbc.AthenaDriver");
            Properties info = new Properties();
            info.put("s3_staging_dir", "s3://dixonaws-athena-result-bucket/test/");
            //info.put("log_path", "/Users/myUser/.athena/athenajdbc.log");
            info.put("aws_credentials_provider_class","com.amazonaws.auth.PropertiesFileCredentialsProvider");
            info.put("aws_credentials_provider_arguments","/tmp/athenaCredentials");

            String databaseName = "fleetbriefing";

            System.out.println("Connecting to Athena...");
            conn = DriverManager.getConnection(athenaUrl, info);

            System.out.println("Listing tables in " + databaseName + "...");
            String sql = "SELECT pickup_location, count(rental_date) AS Rentals, sum(charges) AS Revenue FROM fleetbriefing.invoices_gzip GROUP BY  pickup_location ORDER BY  Rentals DESC LIMIT 3";
            statement = conn.createStatement();
            ResultSet rs = statement.executeQuery(sql);

            while (rs.next()) {
                //Retrieve rental info
                int pickup_location=1;
                int rentals=2;
                int revenue=3;

                String str_pickup_location = rs.getString(pickup_location);

                //Accumulate values
                speechText="Pickup location: " + str_pickup_location + ", with " + rs.getString(rentals) + " rentals and " + rs.getString(revenue) + " in revenue";
            }
            rs.close();
            conn.close();
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            try {
                if (statement != null)
                    statement.close();
            } catch (Exception ex) {

            }
            try {
                if (conn != null)
                    conn.close();
            } catch (Exception ex) {

                ex.printStackTrace();
            }
        }
        */

        speechText="Overall fleet utilization is 89% as of today, which is 2% more than the same time last year, Top three rental locations yesterday were,";
        speechText+="1. Atlanta, with 41 rentals,";
        speechText+="2. Dallas Fort Worth, with 40 rentals,";
        speechText+="3. Grand Rapids, with 40 rentals,";
        speechText+="Would you like to hear more about one of these locations?";

        return(speechText);
    }

    /**
     * Creates a {@code SpeechletResponse} for the hello intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelloResponse() {
        String speechText = "Hello world";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("HelloWorld");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }

    /**
     * Creates a {@code SpeechletResponse} for the location intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getLocationResponse() {
        String speechText = "Detail on Atlanta,";
        speechText += "Total revenue in Atlanta yesterday was 18 thousand dollars, average rental paid was 445 dollars, with 59% business travelers, average rental days is 4, turnaround time was on target at 1 day";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("Atlanta");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        return SpeechletResponse.newTellResponse(speech, card);
    }


    /**
     * Creates a {@code SpeechletResponse} for the help intent.
     *
     * @return SpeechletResponse spoken and visual response for the given intent
     */
    private SpeechletResponse getHelpResponse() {
        String speechText = "You can say hello to me!";

        // Create the Simple card content.
        SimpleCard card = new SimpleCard();
        card.setTitle("HelloWorld");
        card.setContent(speechText);

        // Create the plain text output.
        PlainTextOutputSpeech speech = new PlainTextOutputSpeech();
        speech.setText(speechText);

        // Create reprompt
        Reprompt reprompt = new Reprompt();
        reprompt.setOutputSpeech(speech);

        return SpeechletResponse.newAskResponse(speech, reprompt, card);
    }
}
