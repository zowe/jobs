package org.zowe.jobs.services;

import com.google.gson.JsonElement;
import com.google.gson.JsonObject;

import org.apache.http.HttpEntity;
import org.apache.http.HttpResponse;
import org.apache.http.entity.ContentType;
import org.zowe.api.common.exceptions.NoZosmfResponseEntityException;
import org.zowe.api.common.exceptions.ZoweApiRestException;
import org.zowe.api.common.utils.ResponseUtils;

import java.io.IOException;
import java.net.URI;
import java.util.stream.IntStream;

public abstract class AbstractZosmfRequestRunner<T> {

    public T processResponse(HttpResponse response, URI requestUrl, int... successStatuses) throws IOException {
        int statusCode = ResponseUtils.getStatus(response);
        boolean success = IntStream.of(successStatuses).anyMatch(x -> x == statusCode);
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
