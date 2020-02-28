package eu.einfracentral.service;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.exception.ValidationException;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Service;

@Service
public class SimpleIdCreator implements IdCreator {

    SimpleIdCreator() {
    }

    @Override
    public String createProviderId(Provider provider) {
        if (provider.getId() == null || "".equals(provider.getId())) {
            if (provider.getAcronym() != null && !"".equals(provider.getAcronym())) {
                return StringUtils
                        .stripAccents(provider.getAcronym())
                        .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                        .replace(" ", "_");
            } else {
                return StringUtils
                        .stripAccents(provider.getName())
                        .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                        .replace(" ", "_");
            }
        }
        return StringUtils
                .stripAccents(provider.getId())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_");

    }

    @Override
    public String createServiceId(eu.einfracentral.domain.Service service) {
        if (service.getProviders() == null || service.getProviders().isEmpty() || service.getProviders().get(0).equals("")) {
            throw new ValidationException("Service must have at least 1 Provider.");
        }
        if (service.getName() == null || service.getName().equals("")){
            throw new ValidationException("Service must have a Name.");
        }
        String provider = service.getProviders().get(0);
        return String.format("%s.%s", provider, StringUtils
                .stripAccents(service.getName())
                .replaceAll("[^a-zA-Z0-9\\s\\-\\_]+", "")
                .replace(" ", "_")
                .toLowerCase());
    }
}