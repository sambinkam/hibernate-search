/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.dsl.impl;

import java.time.LocalDate;

import org.hibernate.search.backend.lucene.document.impl.LuceneIndexFieldAccessor;
import org.hibernate.search.backend.lucene.document.model.dsl.impl.LuceneIndexSchemaContext;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaFieldNode;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaNodeCollector;
import org.hibernate.search.backend.lucene.document.model.impl.LuceneIndexSchemaObjectNode;
import org.hibernate.search.backend.lucene.types.codec.impl.LuceneLocalDateFieldCodec;
import org.hibernate.search.backend.lucene.types.converter.impl.LuceneLocalDateFieldConverter;
import org.hibernate.search.backend.lucene.types.predicate.impl.LuceneLocalDateFieldPredicateBuilderFactory;
import org.hibernate.search.backend.lucene.types.projection.impl.LuceneStandardFieldProjectionBuilderFactory;
import org.hibernate.search.backend.lucene.types.sort.impl.LuceneLocalDateFieldSortBuilderFactory;
import org.hibernate.search.engine.backend.document.model.dsl.Sortable;
import org.hibernate.search.engine.backend.document.spi.IndexSchemaFieldDefinitionHelper;

/**
 * @author Guillaume Smet
 */
public class LuceneLocalDateIndexSchemaFieldContext
		extends AbstractLuceneStandardIndexSchemaFieldTypedContext<LuceneLocalDateIndexSchemaFieldContext, LocalDate> {

	private Sortable sortable = Sortable.DEFAULT;

	public LuceneLocalDateIndexSchemaFieldContext(LuceneIndexSchemaContext schemaContext, String relativeFieldName) {
		super( schemaContext, relativeFieldName, LocalDate.class );
	}

	@Override
	public LuceneLocalDateIndexSchemaFieldContext sortable(Sortable sortable) {
		this.sortable = sortable;
		return this;
	}

	@Override
	protected void contribute(IndexSchemaFieldDefinitionHelper<LocalDate> helper, LuceneIndexSchemaNodeCollector collector,
			LuceneIndexSchemaObjectNode parentNode) {
		boolean resolvedSortable = resolveDefault( sortable );
		boolean resolvedProjectable = resolveDefault( projectable );

		LuceneLocalDateFieldConverter converter = new LuceneLocalDateFieldConverter( helper.createUserIndexFieldConverter() );
		LuceneLocalDateFieldCodec codec = new LuceneLocalDateFieldCodec( resolvedProjectable, resolvedSortable );

		LuceneIndexSchemaFieldNode<LocalDate> schemaNode = new LuceneIndexSchemaFieldNode<>(
				parentNode,
				getRelativeFieldName(),
				converter,
				codec,
				new LuceneLocalDateFieldPredicateBuilderFactory( converter ),
				new LuceneLocalDateFieldSortBuilderFactory( resolvedSortable, converter ),
				new LuceneStandardFieldProjectionBuilderFactory<>( resolvedProjectable, codec, converter )
		);

		helper.initialize( new LuceneIndexFieldAccessor<>( schemaNode ) );

		collector.collectFieldNode( schemaNode.getAbsoluteFieldPath(), schemaNode );
	}

	@Override
	protected LuceneLocalDateIndexSchemaFieldContext thisAsS() {
		return this;
	}
}