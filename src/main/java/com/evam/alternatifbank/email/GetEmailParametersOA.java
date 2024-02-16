package com.evam.alternatifbank.email;

import com.evam.sdk.outputaction.AbstractOutputAction;
import com.evam.sdk.outputaction.IOMParameter;
import com.evam.sdk.outputaction.OutputActionContext;
import com.evam.sdk.outputaction.model.DesignerMetaParameters;
import com.evam.sdk.outputaction.model.ReturnParameter;
import com.evam.sdk.outputaction.model.ReturnType;

import java.io.ByteArrayInputStream;
import java.io.FileInputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import com.evam.utils.util.property.FileDefinitions;
import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.client.methods.HttpUriRequest;
import org.apache.http.entity.ByteArrayEntity;
import org.apache.http.impl.client.DefaultHttpClient;
import org.apache.http.params.BasicHttpParams;
import org.apache.http.params.HttpConnectionParams;
import org.apache.http.params.HttpParams;
import org.apache.http.util.EntityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.NodeList;

public class GetEmailParametersOA extends AbstractOutputAction {
    private static final Logger LOGGER = LoggerFactory.getLogger(GetEmailParametersOA.class);
    private static final String contentType = "text/xml; charset=utf-8";
    private static final String INPUT = "INPUT";
    private static final String ID = "Id";
    private static final String EMAIL_TYPE = "EmailType";
    private static final String EMAIL_ADDRESS = "EmailAddress";
    private static final String SEND_EKSTRE = "SendEkstre";
    private static final String SEND_DAILY_EKSTRE = "SendDailyEkstre";
    private static final String INTERNET_BANKING_FLAG = "InternetBankingFlag";
    private static final String CONTACT_EMAIL = "ContactEmail";
    private static final String CONFIRMED_EMAIL = "ConfirmedEmail";
    private static final String EMAIL_PERMITTED = "Email_Permitted";

    private String idValue = "";
    private String emailTypeValue = "";
    private String emailAdressValue = "";
    private String sendEkstreValue = "";
    private String sendDailyEkstreValue = "";
    private String internetBankingFlagValue = "";
    private String contactEmailValue = "";
    private String confirmedEmailValue = "";
    private String emailPermittedValue = "";


    //conf file parameter keys
    private static final String API_URL = "GEP.request_emailInfo_api.url";
    private static final String HOST = "GEP.request_emailInfo_api.host";
    private static final String SOAP_ACTION = "GEP.request_emailInfo_api.soapAction";

    //The following parameters are taken from the properties file
    private static String apiUrl;
    private static String host;
    private static String soapAction;


    @Override
    public synchronized void init() {
        Properties properties = new Properties();
        final String configurationFileName = FileDefinitions.CONF_FOLDER + ActionProperties.ALTERNATIFBANK_CONF_FILE;
        try (FileInputStream fileInputStream = new FileInputStream(configurationFileName)) {
            properties.load(fileInputStream);

            apiUrl = properties.getProperty(API_URL);
            if ((apiUrl == null) || (apiUrl.isEmpty())) {
                LOGGER.warn("GetEmailParametersOA : API_URL dosya da set edilmemiş. " + configurationFileName);
            }
            soapAction = properties.getProperty(SOAP_ACTION);
            if ((soapAction == null) || (soapAction.isEmpty())) {
                LOGGER.warn("GetEmailParametersOA : API_SOAP_ACTION dosya da set edilmemiş. " + configurationFileName);
            }
            host = properties.getProperty(HOST);
            if ((host == null) || (host.isEmpty())) {
                LOGGER.warn("GetEmailParametersOA : API_HOST dosya da set edilmemiş. " + configurationFileName);
            }

        } catch (Exception e) {
            LOGGER.error("GetEmailParametersOA : ERROR {} ", e.toString());
        }
    }

    public int execute(OutputActionContext outputActionContext) {
        String ACTOR_ID = outputActionContext.getActorId();
        String input = (String) outputActionContext.getParameter(INPUT);
        try {
            String soapBody = "<?xml version=\"1.0\" encoding=\"utf-8\"?><soap:Envelope xmlns:xsi=\"http://www.w3.org/2001/XMLSchema-instance\" xmlns:xsd=\"http://www.w3.org/2001/XMLSchema\" xmlns:soap=\"http://schemas.xmlsoap.org/soap/envelope/\"><soap:Body><CustomerSearchMainNew xmlns=\"http://tempuri.org/\"><input>" + input + "</input></CustomerSearchMainNew></soap:Body></soap:Envelope>";

            LOGGER.info("GetEmailParametersOA: Request Body for input " + input + " : " + soapBody);

            DefaultHttpClient defaultHttpClient = new DefaultHttpClient();
            ByteArrayEntity byteArrayEntity = new ByteArrayEntity(soapBody.getBytes("UTF-8"));
            HttpPost httpPost = new HttpPost(apiUrl);
            httpPost.addHeader("SOAPAction", soapAction);
            httpPost.addHeader("Host", host);
            httpPost.addHeader("Content-Type", contentType);
            httpPost.setEntity((HttpEntity) byteArrayEntity);
            BasicHttpParams basicHttpParams = new BasicHttpParams();
            HttpConnectionParams.setConnectionTimeout((HttpParams) basicHttpParams, 30000);
            HttpConnectionParams.setSoTimeout((HttpParams) basicHttpParams, 30000);
            httpPost.setParams((HttpParams) basicHttpParams);
            LOGGER.info("GetEmailParametersOA : ACTOR_ID : " + ACTOR_ID + " Post Request input " + input + " : " + soapBody);
            HttpResponse httpResponse = defaultHttpClient.execute((HttpUriRequest) httpPost);
            String response = EntityUtils.toString(httpResponse.getEntity());
            LOGGER.info("GetEmailParametersOA :ACTOR_ID : " + ACTOR_ID + " Response for input : " + input + " : " + response);

            parseResponse(response);
            EntityUtils.consume(httpResponse.getEntity());

            if (!response.isEmpty()) {
                outputActionContext.getReturnMap().put(ID, this.idValue);
                outputActionContext.getReturnMap().put(EMAIL_TYPE, this.emailTypeValue);
                outputActionContext.getReturnMap().put(EMAIL_ADDRESS, this.emailAdressValue);
                outputActionContext.getReturnMap().put(SEND_EKSTRE, this.sendEkstreValue);
                outputActionContext.getReturnMap().put(SEND_DAILY_EKSTRE, this.sendDailyEkstreValue);
                outputActionContext.getReturnMap().put(INTERNET_BANKING_FLAG, this.internetBankingFlagValue);
                outputActionContext.getReturnMap().put(CONTACT_EMAIL, this.contactEmailValue);
                outputActionContext.getReturnMap().put(CONFIRMED_EMAIL, this.confirmedEmailValue);
                outputActionContext.getReturnMap().put(EMAIL_PERMITTED, this.emailPermittedValue);
                return 0;
            }

        } catch (Exception e) {
            LOGGER.error("An unexpected error occured. " + e);
            e.printStackTrace();
        }
        return -1;
    }

    private void parseResponse(String response) {
        try {
            DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
            DocumentBuilder builder = factory.newDocumentBuilder();
            Document doc = builder.parse(new ByteArrayInputStream(response.getBytes()));

            NodeList customerEmailsObList = doc.getElementsByTagName("CustomerEmailsOb");

            if (customerEmailsObList.getLength() > 0) {
                Element customerEmailsOb = (Element) customerEmailsObList.item(0);

                String id = customerEmailsOb.getElementsByTagName(ID).item(0).getTextContent();
                String emailType = customerEmailsOb.getElementsByTagName(EMAIL_TYPE).item(0).getTextContent();
                String emailAddress = customerEmailsOb.getElementsByTagName(EMAIL_ADDRESS).item(0).getTextContent();
                String sendEkstre = customerEmailsOb.getElementsByTagName(SEND_EKSTRE).item(0).getTextContent();
                String sendDailyEkstre = customerEmailsOb.getElementsByTagName(SEND_DAILY_EKSTRE).item(0).getTextContent();
                String internetBankingFlag = customerEmailsOb.getElementsByTagName(INTERNET_BANKING_FLAG).item(0).getTextContent();
                String contactEmail = customerEmailsOb.getElementsByTagName(CONTACT_EMAIL).item(0).getTextContent();
                String confirmedEmail = customerEmailsOb.getElementsByTagName(CONFIRMED_EMAIL).item(0).getTextContent();
                String emailPermitted = customerEmailsOb.getElementsByTagName(EMAIL_PERMITTED).item(0).getTextContent();

                this.idValue = id;
                this.emailTypeValue = emailType;
                this.emailAdressValue = emailAddress;
                this.sendEkstreValue = sendEkstre;
                this.sendDailyEkstreValue = sendDailyEkstre;
                this.internetBankingFlagValue = internetBankingFlag;
                this.contactEmailValue = contactEmail;
                this.confirmedEmailValue = confirmedEmail;
                this.emailPermittedValue = emailPermitted;

            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    protected List<IOMParameter> getParameters() {
        List<IOMParameter> parameters = new ArrayList<>();
        parameters.add(new IOMParameter(INPUT, "Sorgulama yapılan"));
        return parameters;
    }

    public boolean isReturnable() {
        return true;
    }

    public ReturnParameter[] getRetParams(DesignerMetaParameters designerMetaParameters) {

        ReturnParameter id = new ReturnParameter(ID, ReturnType.String);
        ReturnParameter emailType = new ReturnParameter(EMAIL_TYPE, ReturnType.String);
        ReturnParameter emailAdress = new ReturnParameter(EMAIL_ADDRESS, ReturnType.String);
        ReturnParameter sendEkstre = new ReturnParameter(SEND_EKSTRE, ReturnType.String);
        ReturnParameter sendDailyEkstre = new ReturnParameter(SEND_DAILY_EKSTRE, ReturnType.String);
        ReturnParameter internetBankingFlag = new ReturnParameter(INTERNET_BANKING_FLAG, ReturnType.String);
        ReturnParameter contactEmail = new ReturnParameter(CONTACT_EMAIL, ReturnType.String);
        ReturnParameter confirmedEmail = new ReturnParameter(CONFIRMED_EMAIL, ReturnType.String);
        ReturnParameter emailPermitted = new ReturnParameter(EMAIL_PERMITTED, ReturnType.String);


        return new ReturnParameter[]{id, emailType, emailAdress, sendEkstre, sendDailyEkstre, internetBankingFlag, contactEmail, confirmedEmail, emailPermitted};
    }

    public boolean actionInputStringShouldBeEvaluated() {
        return false;
    }

    public String getVersion() {
        return "1.0";
    }
}
