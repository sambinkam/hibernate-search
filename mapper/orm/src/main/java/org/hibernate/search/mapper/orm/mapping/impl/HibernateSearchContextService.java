/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.mapper.orm.mapping.impl;

import java.lang.invoke.MethodHandles;

import org.hibernate.engine.spi.SessionFactoryImplementor;
import org.hibernate.search.engine.common.spi.SearchIntegration;
import org.hibernate.search.mapper.orm.logging.impl.Log;
import org.hibernate.search.mapper.orm.mapping.spi.HibernateOrmMapping;
import org.hibernate.search.util.common.logging.impl.LoggerFactory;
import org.hibernate.service.Service;

public final class HibernateSearchContextService implements Service, AutoCloseable {

	public static HibernateSearchContextService get(SessionFactoryImplementor sessionFactory) {
		return sessionFactory.getServiceRegistry().getService( HibernateSearchContextService.class );
	}

	private volatile SearchIntegration integration;
	private volatile HibernateOrmMappingImpl mapping;

	@Override
	public void close() {
		if ( integration != null ) {
			integration.close();
		}
	}

	public void initialize(SearchIntegration integration, HibernateOrmMappingImpl mapping) {
		this.integration = integration;
		this.mapping = mapping;
	}

	public SearchIntegration getIntegration() {
		if ( integration != null ) {
			return integration;
		}
		else {
			throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
		}
	}

	public HibernateOrmMapping getMapping() {
		if ( mapping != null ) {
			return mapping;
		}
		else {
			throw LoggerFactory.make( Log.class, MethodHandles.lookup() ).hibernateSearchNotInitialized();
		}
	}

}