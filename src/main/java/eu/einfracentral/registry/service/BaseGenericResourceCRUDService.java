package eu.einfracentral.registry.service;

import eu.einfracentral.domain.Identifiable;
import eu.openminted.registry.core.domain.Browsing;
import eu.openminted.registry.core.domain.FacetFilter;
import eu.openminted.registry.core.domain.Resource;
import eu.openminted.registry.core.service.*;
import org.apache.log4j.Logger;

import java.net.UnknownHostException;
import java.util.Date;
import java.util.concurrent.ExecutionException;

/**
 * Created by pgl on 12/7/2017.
 */
public abstract class BaseGenericResourceCRUDService<T extends Identifiable> extends AbstractGenericService<T> implements ResourceCRUDService<T> {
    private Logger logger;

    public BaseGenericResourceCRUDService(Class<T> typeParameterClass) {
        super(typeParameterClass);
        logger = Logger.getLogger(typeParameterClass);
    }

    @Override
    public T get(String resourceID) {
        T serialized;
        try {
            String type = getResourceType();
            String idFieldName = String.format("%s_id", type);
            Resource found = searchService.searchId(type, new SearchService.KeyValue(idFieldName, resourceID));
            serialized = parserPool.serialize(found, typeParameterClass).get();
        } catch (UnknownHostException | InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        return serialized;
    }

    @Override
    public Browsing getAll(FacetFilter facetFilter) {
        facetFilter.setBrowseBy(getBrowseBy());
        return getResults(facetFilter);
    }

    @Override
    public Browsing getMy(FacetFilter facetFilter) {
        return null;
    }

    @Override
    public void update(T newResource) {
        Resource resourceFound;
        Resource resource = new Resource();
        try {
            resourceFound = searchService.searchId(getResourceType(), new SearchService.KeyValue(getResourceType() + "_id", "" + newResource.getId()));
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if (resourceFound == null) {
            throw new ServiceException(String.format("Resource doesn't exist: {type: %s, id: %s}", getResourceType(), newResource.getId()));
        } else {
            try {
                String serialized = parserPool.deserialize(newResource, ParserService.ParserServiceTypes.XML).get();
                if (!serialized.equals("failed")) {
                    resource.setPayload(serialized);
                } else {
                    throw new ServiceException("Serialization failed");
                }
                resource = (Resource) resourceFound;
                resource.setPayloadFormat("xml");
                resource.setPayload(serialized);
                resourceService.updateResource(resource);
            } catch (ExecutionException | InterruptedException e) {
                logger.fatal(e);
                throw new ServiceException(e);
            }
        }
    }

    private String getFieldIDName() {
        return String.format("%s_id", getResourceType());
    }

    @Override
    public void add(T resourceToAdd) {
        try {
            Resource found = searchService.searchId(getResourceType(), new SearchService.KeyValue(getFieldIDName(), resourceToAdd.getId()));
            if (found != null) {
                throw new ServiceException(String.format("Resource already exists: {type: %s, id: %s}", getResourceType(), resourceToAdd.getId()));
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
        Resource created = new Resource();
        String deserialized = null;
        try {
            deserialized = parserPool.deserialize(resourceToAdd, ParserService.ParserServiceTypes.XML).get();
        } catch (InterruptedException | ExecutionException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }

        if (!deserialized.equals("failed")) {
            created.setPayload(deserialized);
        } else {
            throw new ServiceException("Serialization failed");
        }
        created.setCreationDate(new Date());
        created.setModificationDate(new Date());
        created.setPayloadFormat("xml");
        created.setResourceType(getResourceType());
        created.setVersion("not_set");
        created.setId("wont be saved");

        resourceService.addResource(created);
    }

    @Override
    public void delete(T resourceToDelete) {
        Resource resourceFound;
        try {
            resourceFound = searchService.searchId(getResourceType(), new SearchService.KeyValue(getResourceType() + "_id", "" + resourceToDelete.getId()));
            if (resourceFound == null) {
                throw new ServiceException(String.format("Resource doesn't exist: {type: %s, id: %s}", getResourceType(), resourceToDelete.getId()));
            } else {
                resourceService.deleteResource(resourceFound.getId());
            }
        } catch (UnknownHostException e) {
            logger.fatal(e);
            throw new ServiceException(e);
        }
    }
}
