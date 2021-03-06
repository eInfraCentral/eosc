package eu.einfracentral.service;

import eu.einfracentral.domain.Provider;
import eu.einfracentral.domain.Service;

public interface IdCreator {

    /**
     * Creates id for {@link Provider}
     *
     * @param provider
     * @return
     */
    String createProviderId(Provider provider);

    /**
     * Creates id for {@link Service}
     *
     * @param service
     * @return
     */
    String createServiceId(Service service);
}
