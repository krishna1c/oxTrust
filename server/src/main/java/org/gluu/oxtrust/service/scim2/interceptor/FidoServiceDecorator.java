package org.gluu.oxtrust.service.scim2.interceptor;

import org.apache.commons.lang.StringUtils;
import org.gluu.oxtrust.ldap.service.IFidoDeviceService;
import org.gluu.oxtrust.model.exception.SCIMException;
import org.gluu.oxtrust.model.fido.GluuCustomFidoDevice;
import org.gluu.oxtrust.model.scim2.ErrorScimType;
import org.gluu.oxtrust.model.scim2.SearchRequest;
import org.gluu.oxtrust.model.scim2.fido.FidoDeviceResource;
import org.gluu.oxtrust.model.scim2.util.ResourceValidator;
import org.gluu.oxtrust.service.scim2.ExtensionService;
import org.gluu.oxtrust.ws.rs.scim2.BaseScimWebService;
import org.gluu.oxtrust.ws.rs.scim2.FidoDeviceService;
import org.slf4j.Logger;

import javax.decorator.Delegate;
import javax.enterprise.inject.Any;
import javax.inject.Inject;
import javax.ws.rs.core.Response;
import java.util.Collections;

import static org.gluu.oxtrust.model.scim2.Constants.SEARCH_REQUEST_SCHEMA_ID;

/**
 * Aims at decorating SCIM service methods. Currently applies validations via ResourceValidator class or other custom
 * validation logic
 *
 * Created by jgomer on 2017-10-09.
 */
public abstract class FidoServiceDecorator extends BaseScimWebService implements FidoDeviceService {

    @Inject
    private Logger log;

    @Inject @Delegate @Any
    FidoDeviceService service;

    @Inject
    private ExtensionService extService;

    @Inject
    private IFidoDeviceService fidoDeviceService;

    private Response validateExistenceOfDevice(String userId, String id) {

        Response response=null;

        GluuCustomFidoDevice device = fidoDeviceService.getGluuCustomFidoDeviceById(userId, id);
        if (device == null) {
            log.info("Device with id {} not found", id);
            response = getErrorResponse(Response.Status.NOT_FOUND, ErrorScimType.INVALID_VALUE, "Resource " + id + " not found");
        }
        return response;

    }

    public Response getDeviceById(String id, String userId, String attrsList, String excludedAttrsList, String authorization){
        Response response=validateExistenceOfDevice(userId, id);
        if (response==null)
            response=service.getDeviceById(id, userId, attrsList, excludedAttrsList, authorization);
        return response;

    }

    public Response updateDevice(FidoDeviceResource fidoDevice, String id, String attrsList, String excludedAttrsList, String authorization){

        Response response;
        try {
            ResourceValidator rv=new ResourceValidator(fidoDevice, extService.getResourceExtensions(fidoDevice.getClass()));
            rv.validateRequiredAttributes();
            rv.validateSchemasAttribute();
            rv.validateValidableAttributes();

            if (!fidoDevice.equals(id))
                throw new SCIMException("Parameter id does not match id attribute of Device");

            response=validateExistenceOfDevice(fidoDevice.getUserId(), id);
            if (response==null)
                response=service.updateDevice(fidoDevice, id, attrsList, excludedAttrsList, authorization);
        }
        catch (SCIMException e){
            log.error("Validation check at updateDevice returned: {}", e.getMessage());
            response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_VALUE, e.getMessage());
        }
        return response;

    }

    public Response deleteDevice(String id, String authorization){

        Response response=validateExistenceOfDevice(null, id);
        if (response==null)
            response=service.deleteDevice(id, authorization);
        return response;

    }

    public Response searchDevices(String filter, String sortBy, String sortOrder, Integer startIndex, Integer count,
                                    String attrsList, String excludedAttrsList, String authorization) {

        SearchRequest searchReq=new SearchRequest();
        Response response=prepareSearchRequest(Collections.singletonList(SEARCH_REQUEST_SCHEMA_ID), filter, sortBy, sortOrder,
                                startIndex, count, attrsList, excludedAttrsList, searchReq, "id");

        if (response==null) {
            if (!isAttributeRecognized(FidoDeviceResource.class, searchReq.getSortBy()))
                response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_PATH, "sortBy parameter value not recognized");
            else
                response = service.searchDevices(searchReq.getFilter(), searchReq.getSortBy(), searchReq.getSortOrder(),
                        searchReq.getStartIndex(), searchReq.getCount(), searchReq.getAttributes(),
                        searchReq.getExcludedAttributes(), authorization);
        }
        return response;

    }

    public Response searchDevicesPost(SearchRequest searchRequest, String authorization) {

        SearchRequest searchReq = new SearchRequest();
        Response response = prepareSearchRequest(searchRequest.getSchemas(), searchRequest.getFilter(), searchRequest.getSortBy(),
                                searchRequest.getSortOrder(), searchRequest.getStartIndex(), searchRequest.getCount(),
                                searchRequest.getAttributes(), searchRequest.getExcludedAttributes(), searchReq, "id");

        if (response==null) {
            if (!isAttributeRecognized(FidoDeviceResource.class, searchReq.getSortBy()))
                response = getErrorResponse(Response.Status.BAD_REQUEST, ErrorScimType.INVALID_PATH, "sortBy parameter value not recognized");
            else
                response = service.searchDevicesPost(searchReq, authorization);
        }
        return response;
    }

}
