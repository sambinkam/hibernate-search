/*
 * Hibernate Search, full-text search for your domain model
 *
 * License: GNU Lesser General Public License (LGPL), version 2.1 or later
 * See the lgpl.txt file in the root directory or <http://www.gnu.org/licenses/lgpl-2.1.html>.
 */
package org.hibernate.search.engine.search.sort.dsl;


import org.hibernate.search.engine.search.SearchSort;

/**
 * The final step in a sort definition, where the sort can be retrieved.
 */
public interface SortFinalStep {

	/**
	 * Create a {@link SearchSort} instance
	 * matching the definition given in the previous DSL steps.
	 *
	 * @return The {@link SearchSort} resulting from the previous DSL steps.
	 */
	SearchSort toSort();

}