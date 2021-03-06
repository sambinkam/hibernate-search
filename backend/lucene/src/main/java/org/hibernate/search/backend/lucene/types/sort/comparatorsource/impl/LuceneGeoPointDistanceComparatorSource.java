/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.backend.lucene.types.sort.comparatorsource.impl;

import java.io.IOException;

import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.GeoPointDistanceMultiValuesToSingleValuesSource;
import org.hibernate.search.backend.lucene.lowlevel.docvalues.impl.MultiValueMode;
import org.hibernate.search.engine.spatial.GeoPoint;

import org.apache.lucene.index.LeafReaderContext;
import org.apache.lucene.index.NumericDocValues;
import org.apache.lucene.search.FieldComparator;
import org.apache.lucene.search.Query;

public class LuceneGeoPointDistanceComparatorSource extends LuceneFieldComparatorSource {

	private static final double MISSING_VALUE_IMPLICIT_DISTANCE_VALUE = Double.POSITIVE_INFINITY;

	private final GeoPoint center;
	private final MultiValueMode mode;

	public LuceneGeoPointDistanceComparatorSource(String nestedDocumentPath, GeoPoint center, MultiValueMode mode, Query filter) {
		super( nestedDocumentPath, filter );
		this.center = center;
		this.mode = mode;
	}

	@Override
	public FieldComparator<?> newComparator(String fieldname, int numHits, int sortPos, boolean reversed) {
		GeoPointDistanceMultiValuesToSingleValuesSource source = new GeoPointDistanceMultiValuesToSingleValuesSource(
				fieldname, mode, nestedDocsProvider, center
		);

		return new FieldComparator.DoubleComparator( numHits, fieldname, MISSING_VALUE_IMPLICIT_DISTANCE_VALUE ) {
			@Override
			protected NumericDocValues getNumericDocValues(LeafReaderContext context, String field) throws IOException {
				return source.getValues( context, null ).getRawDoubleValues();
			}
		};
	}
}
