package eu.einfracentral.registry.manager;

import eu.einfracentral.domain.*;
import eu.einfracentral.exception.ResourceException;
import eu.einfracentral.registry.service.*;
import eu.openminted.registry.core.domain.Resource;
import java.util.*;
import java.util.concurrent.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;

@org.springframework.stereotype.Service("serviceService")
public class ServiceManager extends ResourceManager<Service> implements ServiceService {
    @Autowired
    private AddendaManager addendaManager;

    public ServiceManager() {
        super(Service.class);
    }

    @Override
    public String getResourceType() {
        return "service";
    }

    @Override
    public Service add(Service service) {
        //TODO: id is null when service is added via frontend, so make sure to make one, based on provider
        if (!service.getId().contains(".")) {
            service.setId(java.util.UUID.randomUUID().toString());
        }
        if (exists(service)) {
            throw new ResourceException(String.format("%s already exists!", resourceType.getName()), HttpStatus.CONFLICT);
        }
        return super.add(validate(service));
    }

    private Service fixVersion(Service service) {
        if (service.getVersion() == null || service.getVersion().equals("")) {
            service.setVersion("0");
        }
        return service;
    }

    @Override
    public Service update(Service service) {
        service = validate(service);
        Service existingService = get(service.getId());
        fixVersion(existingService); //remove this when it has ran for all services
        updateAddenda(service.getId());
        return service.getVersion().equals(existingService.getVersion()) ? super.update(service) : super.add(service);
    }

    private Addenda ensureAddenda(String id) {
        try {
            return parserPool.deserialize(addendaManager.where("service", id, true), Addenda.class).get();
        } catch (InterruptedException | ExecutionException | ResourceException e) {
            e.printStackTrace();
            return addAddenda(id);
        }
    }

    private Addenda makeAddenda(String id) {
        Addenda ret = new Addenda();
        ret.setId(UUID.randomUUID().toString());
        ret.setService(id);
        try {
            addendaManager.add(ret);
        } catch (Throwable e) {
            e.printStackTrace();
        }
        return ret;
    }

    private void updateAddenda(String id) {
        try {
            Addenda addenda = ensureAddenda(id);
            addenda.setModifiedAt(System.currentTimeMillis());
            addenda.setModifiedBy("pgl");
            addendaManager.update(addenda);
        } catch (Throwable e) {
            e.printStackTrace(); //addenda are thoroughly optional, and should not interfere with normal add/update operations
        }
    }

    @Override
    public Service validate(Service service) {
        return fixVersion(service);
    }
}
