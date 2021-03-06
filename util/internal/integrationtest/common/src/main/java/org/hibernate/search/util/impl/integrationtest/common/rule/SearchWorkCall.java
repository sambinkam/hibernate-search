/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.util.impl.integrationtest.common.rule;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import org.hibernate.search.engine.search.loading.context.spi.LoadingContext;
import org.hibernate.search.engine.search.query.SearchResult;
import org.hibernate.search.engine.search.loading.spi.LoadingResult;
import org.hibernate.search.engine.search.loading.spi.ProjectionHitMapper;
import org.hibernate.search.engine.search.query.spi.SimpleSearchResult;
import org.hibernate.search.util.impl.integrationtest.common.assertion.StubSearchWorkAssert;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.StubSearchWork;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjection;
import org.hibernate.search.util.impl.integrationtest.common.stub.backend.search.projection.impl.StubSearchProjectionContext;

class SearchWorkCall<T> extends Call<SearchWorkCall<?>> {

	private final Set<String> indexNames;
	private final StubSearchWork work;
	private final StubSearchProjectionContext projectionContext;
	private final LoadingContext<?, ?> loadingContext;
	private final StubSearchProjection<T> rootProjection;
	private final StubSearchWorkBehavior<?> behavior;

	SearchWorkCall(Set<String> indexNames,
			StubSearchWork work,
			StubSearchProjectionContext projectionContext,
			LoadingContext<?, ?> loadingContext,
			StubSearchProjection<T> rootProjection) {
		this.indexNames = indexNames;
		this.work = work;
		this.projectionContext = projectionContext;
		this.loadingContext = loadingContext;
		this.rootProjection = rootProjection;
		this.behavior = null;
	}

	SearchWorkCall(Set<String> indexNames,
			StubSearchWork work,
			StubSearchWorkBehavior<?> behavior) {
		this.indexNames = indexNames;
		this.work = work;
		this.projectionContext = null;
		this.loadingContext = null;
		this.rootProjection = null;
		this.behavior = behavior;
	}

	public <U> CallBehavior<SearchResult<U>> verify(SearchWorkCall<U> actualCall) {
		assertThat( actualCall.indexNames )
				.as( "Search work did not target the expected indexes: " )
				.isEqualTo( indexNames );
		StubSearchWorkAssert.assertThat( actualCall.work )
				.as( "Search work on indexes " + indexNames + " did not match: " )
				.matches( work );

		return () -> new SimpleSearchResult<>(
				behavior.getTotalHitCount(),
				getResults(
						actualCall.projectionContext,
						actualCall.loadingContext.createProjectionHitMapper(),
						actualCall.rootProjection,
						behavior.getRawHits()
				),
				Collections.emptyMap(),
				Duration.ZERO, false
		);
	}

	@Override
	protected boolean isSimilarTo(SearchWorkCall<?> other) {
		return Objects.equals( indexNames, other.indexNames );
	}

	private static <H> List<H> getResults(StubSearchProjectionContext actualProjectionContext,
			ProjectionHitMapper<?, ?> actualProjectionHitMapper,
			StubSearchProjection<H> actualRootProjection,
			List<?> rawHits) {
		List<Object> extractedElements = new ArrayList<>( rawHits.size() );

		for ( Object rawHit : rawHits ) {
			actualProjectionContext.reset();
			extractedElements.add(
					actualRootProjection.extract( actualProjectionHitMapper, rawHit, actualProjectionContext )
			);
		}

		LoadingResult<?> loadingResult = actualProjectionHitMapper.loadBlocking();

		List<H> hits = new ArrayList<>( rawHits.size() );

		for ( Object extractedElement : extractedElements ) {
			actualProjectionContext.reset();

			H hit = actualRootProjection.transform( loadingResult, extractedElement, actualProjectionContext );
			if ( actualProjectionContext.hasFailedLoad() ) {
				// skip this hit
				continue;
			}

			hits.add( hit );
		}

		return hits;
	}

	@Override
	public String toString() {
		return "search work execution on indexes '" + indexNames + "'; work = " + work;
	}

}
