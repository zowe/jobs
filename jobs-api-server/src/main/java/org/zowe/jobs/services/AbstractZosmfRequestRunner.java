package org.zowe.jobs.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import lombok.extern.slf4j.Slf4j;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.client.methods.RequestBuilder;
import org.apache.http.entity.ContentType;
import org.zowe.api.common.connectors.zosmf.ZosmfConnector;
import org.zowe.api.common.exceptions.NoZosmfResponseEntityException;
import org.zowe.api.common.exceptions.ServerErrorException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.stream.IntStream;

@Slf4j
public abstract class AbstractZosmfRequestRunner<T> {

    public T run(ZosmfConnector zosmfconnector) {
        try {
            RequestBuilder requestBuilder = prepareQuery(zosmfconnector);
            URI uri = requestBuilder.getUri();
            HttpResponse response = zosmfconnector.request(requestBuilder);
            return processResponse(response, uri);
        } catch (IOException | URISyntaxException e) {
            log.error("run", e);
            throw new ServerErrorException(e);
        }
    }

    abstract int[] getSuccessStatus();

    abstract RequestBuilder prepareQuery(ZosmfConnector zosmfconnector) throws URISyntaxException, IOException;

    T processResponse(HttpResponse response, URI requestUrl) throws IOException {
        int statusCode = ResponseUtils.getStatus(response);
        boolean success = IntStream.of(getSuccessStatus()).anyMatch(x -> x == statusCode);
        if (success) {
            return getResult(response);
        } else {
            throw createGeneralException(response, statusCode, requestUrl);
        }
    }

    abstract T getResult(HttpResponse response) throws IOException;

    ZoweApiRestException createException(JsonObject jsonResponse, int statusCode) {
        return null;
    }

    // TODO - consider adding requestUrl to responseCache?
    private ZoweApiRestException createGeneralException(HttpResponse response, int statusCode, URI requestUrl)
            throws IOException {
        HttpEntity entity = response.getEntity();
        if (entity != null) {
            ContentType contentType = ContentType.get(entity);
            String mimeType = contentType.getMimeType();
            if (mimeType.equals(ContentType.APPLICATION_JSON.getMimeType())) {
                JsonObject jsonResponse = ResponseUtils.getEntityAsJsonObject(response);

                ZoweApiRestException exception = createException(jsonResponse, statusCode);
                if (exception != null) {
                    return exception;
                }
                if (jsonResponse.has("message")) {
                    String zosmfMessage = jsonResponse.get("message").getAsString();
                    return new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), zosmfMessage);
                }
                return new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), jsonResponse.toString());
            } else {
                return new ZoweApiRestException(getSpringHttpStatusFromCode(statusCode), entity.toString());
            }
        } else {
            return new NoZosmfResponseEntityException(getSpringHttpStatusFromCode(statusCode), requestUrl.toString());
        }
    }

    static String getStringOrNull(JsonObject json, String key) {
        String value = null;
        JsonElement jsonElement = json.get(key);
        if (!jsonElement.isJsonNull()) {
            value = jsonElement.getAsString();
        }
        return value;
    }

    private org.springframework.http.HttpStatus getSpringHttpStatusFromCode(int statusCode) {
        return org.springframework.http.HttpStatus.resolve(statusCode);
    }
}
