/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.integrationtest.mapper.orm.automaticindexing;

import static org.hibernate.search.util.impl.test.FutureAssert.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Id;

import org.hibernate.SessionFactory;
import org.hibernate.search.engine.backend.index.spi.DocumentRefreshStrategy;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmAutomaticIndexingSynchronizationStrategyName;
import org.hibernate.search.mapper.orm.cfg.HibernateOrmMapperSettings;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.GenericField;
import org.hibernate.search.mapper.pojo.mapping.definition.annotation.Indexed;
import org.hibernate.search.util.impl.integrationtest.common.rule.BackendMock;
import org.hibernate.search.util.impl.integrationtest.orm.OrmSetupHelper;
import org.hibernate.search.util.impl.integrationtest.orm.OrmUtils;

import org.junit.Rule;
import org.junit.Test;

public class AutomaticIndexingSynchronizationStrategyIT {

	// Let's say 3 seconds are long enough to consider that, if nothing changed after this time, nothing ever will.
	private static final long ALMOST_FOREVER_VALUE = 3L;
	private static final TimeUnit ALMOST_FOREVER_UNIT = TimeUnit.SECONDS;

	@Rule
	public BackendMock backendMock = new BackendMock( "stubBackend" );

	@Rule
	public OrmSetupHelper ormSetupHelper = new OrmSetupHelper();

	@Test
	public void queued() throws InterruptedException, ExecutionException, TimeoutException {
		SessionFactory sessionFactory = setup( HibernateOrmAutomaticIndexingSynchronizationStrategyName.QUEUED );
		testAsynchronous( sessionFactory, DocumentRefreshStrategy.NONE );
	}

	@Test
	public void committed_default() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( null );
		testSynchronous( sessionFactory, DocumentRefreshStrategy.NONE );
	}

	@Test
	public void committed_explicit() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( HibernateOrmAutomaticIndexingSynchronizationStrategyName.COMMITTED );
		testSynchronous( sessionFactory, DocumentRefreshStrategy.NONE );
	}

	@Test
	public void searchable() throws InterruptedException, TimeoutException, ExecutionException {
		SessionFactory sessionFactory = setup( HibernateOrmAutomaticIndexingSynchronizationStrategyName.SEARCHABLE );
		testSynchronous( sessionFactory, DocumentRefreshStrategy.FORCE );
	}

	private void testSynchronous(SessionFactory sessionFactory,
			DocumentRefreshStrategy expectedRefreshStrategy)
			throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<?> workFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionFuture = runTransactionInDifferentThread(
				sessionFactory,
				expectedRefreshStrategy,
				workFuture,
				/*
				 * With this synchronization strategy, the transaction may NOT unblock the thread
				 * until the work future is complete.
				 */
				() -> assertThat( workFuture ).isSuccessful()
		);

		// We expect the transaction to block forever, because the work future isn't complete
		ALMOST_FOREVER_UNIT.sleep( ALMOST_FOREVER_VALUE );
		assertThat( transactionFuture ).isPending();

		// Completing the work should allow the transaction to unblock the thread
		workFuture.complete( null );
		/*
		 * Note that this will throw an ExecutionException it the transaction failed
		 * or an assertion failed in the other thread.
		 */
		transactionFuture.get( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT );
		assertThat( transactionFuture ).isSuccessful();
	}

	private void testAsynchronous(SessionFactory sessionFactory,
			DocumentRefreshStrategy expectedRefreshStrategy)
			throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<?> workFuture = new CompletableFuture<>();

		CompletableFuture<?> transactionFuture = runTransactionInDifferentThread(
				sessionFactory,
				expectedRefreshStrategy,
				workFuture,
				/*
				 * With this synchronization strategy, the transaction will unblock the thread
				 * before the work future is complete.
				 */
				() -> assertThat( workFuture ).isPending()
		);

		/*
		 * We didn't complete the work, but the transaction should unblock the thread anyway.
		 * Note that this will throw an ExecutionException it the transaction failed
		 * or an assertion failed in the other thread.
		 */
		transactionFuture.get( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT );
	}

	/*
	 * Run a transaction in a different thread so that its progress can be inspected from the current thread.
	 */
	private CompletableFuture<?> runTransactionInDifferentThread(SessionFactory sessionFactory,
			DocumentRefreshStrategy expectedRefreshStrategy,
			CompletableFuture<?> workFuture,
			Runnable afterTransactionAssertion)
			throws InterruptedException, ExecutionException, TimeoutException {
		CompletableFuture<?> justBeforeTransactionCommitFuture = new CompletableFuture<>();
		CompletableFuture<?> transactionFuture = CompletableFuture.runAsync( () -> {
			OrmUtils.withinTransaction( sessionFactory, session -> {
				IndexedEntity entity1 = new IndexedEntity();
				entity1.setId( 1 );
				entity1.setIndexedField( "initialValue" );

				session.persist( entity1 );

				backendMock.expectWorks( IndexedEntity.INDEX, expectedRefreshStrategy )
						.add( "1", b -> b
								.field( "indexedField", entity1.getIndexedField() )
						)
						.preparedThenExecuted( workFuture );
				justBeforeTransactionCommitFuture.complete( null );
			} );
			backendMock.verifyExpectationsMet();

			afterTransactionAssertion.run();
		} );

		// Ensure the transaction at least reached the point just before commit
		justBeforeTransactionCommitFuture.get( ALMOST_FOREVER_VALUE, ALMOST_FOREVER_UNIT );

		return transactionFuture;
	}

	private SessionFactory setup(HibernateOrmAutomaticIndexingSynchronizationStrategyName strategyName) {
		OrmSetupHelper.SetupContext setupContext = ormSetupHelper.withBackendMock( backendMock );
		if ( strategyName != null ) {
			setupContext.withProperty(
					HibernateOrmMapperSettings.AUTOMATIC_INDEXING_SYNCHRONIZATION_STRATEGY,
					strategyName
			);
		}

		backendMock.expectSchema( IndexedEntity.INDEX, b -> b
				.field( "indexedField", String.class )
		);
		SessionFactory sessionFactory = setupContext.setup( IndexedEntity.class );
		backendMock.verifyExpectationsMet();

		return sessionFactory;
	}

	@Entity(name = "indexed")
	@Indexed(index = IndexedEntity.INDEX)
	public static class IndexedEntity {

		static final String INDEX = "IndexedEntity";

		@Id
		private Integer id;

		@Basic
		@GenericField
		private String indexedField;

		public Integer getId() {
			return id;
		}

		public void setId(Integer id) {
			this.id = id;
		}

		public String getIndexedField() {
			return indexedField;
		}

		public void setIndexedField(String indexedField) {
			this.indexedField = indexedField;
		}

	}
}