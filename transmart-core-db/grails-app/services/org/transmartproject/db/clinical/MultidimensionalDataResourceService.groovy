package org.transmartproject.db.clinical

import grails.gorm.DetachedCriteria
import grails.orm.HibernateCriteriaBuilder
import groovy.transform.TupleConstructor
import org.apache.commons.lang.NotImplementedException
import org.hibernate.Criteria
import org.hibernate.ScrollMode
import org.hibernate.ScrollableResults
import org.hibernate.Session
import org.hibernate.SessionFactory
import org.hibernate.StatelessSession
import org.hibernate.engine.spi.SessionImplementor
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.stereotype.Component
import org.transmartproject.db.dataquery2.Dimension
import org.transmartproject.db.dataquery2.Hypercube
import org.transmartproject.db.i2b2data.ObservationFact
import org.transmartproject.db.i2b2data.Study


class MultidimensionalDataResourceService {

    @Autowired
    SessionFactory sessionFactory

    static final int NUM_FIXED_PROJECTIONS = 2

    Hypercube doQuery(Map args) {
        def constraints = args.constraints
        def sort = args.sort
        def pack = args.pack
        def preloadDimensions = args.pack ?: false

        def studynames = [constraints?.study]
        if(!studynames?.get(0)) {
            throw new RuntimeException("no study provided")
        }

        DetachedCriteria<ObservationFact> q = ObservationFact.where {
            trialvisit.study.name in studynames
        }

        if(constraints?.study) {
            q = q.where {
                trialVisit.study.name == constraints.study
            }
        }

        // This throws a LegacyStudyException for non-17.1 style studies
        List<Dimension> dimensions = Study.findAll {
            name in studynames
        }*.dimensions.flatten()*.dimension.unique()


        def projection = {
            // NUM_FIXED_PROJECTIONS must match the number of projections defined here
            textValue 'textValue'
            numberValue 'numberValue'
        }
        Query query = new Query(q, [modifierCodes: ['@']], [projection], [])

        dimensions.each {
            it.selectIDs(query)
        }
        if (query.params.mofifierCodes != ['@']) throw new NotImplementedException("Modifer dimensions are not yet implemented")

        q = query.criteria.where {
            projection {
                query.projection.each {
                    it.delegate = delegate
                    it()
                    it.delegate = null
                }
            }
        }

        def hibernateCriteria = HibernateCriteriaBuilder.getHibernateDetachedCriteria(
                /*not used in this method's implementation*/ null, q)

        def session = sessionFactory.openStatelessSession()
        session.connection().autoCommit = false

        // we need to access a private here, the official getExecutableCriteria method requires a Session which
        // it casts to a SessionImplementor
        Criteria criteria = hibernateCriteria.criteriaImpl
        criteria.session = (SessionImplementor) session

        ScrollableResults results = criteria.scroll(ScrollMode.FORWARD_ONLY)

        new Hypercube(results, dimensions, query, session)
        // session will be closed by the Hypercube

    }

    /*
    note: efficiently extracting the available dimension elements for dimensions is possible using nonstandard
    sql. For Postgres:
    SELECT array_agg(DISTINCT patient_num), array_agg(DISTINCT concept_cd),
        array_agg(distinct case when modifier_cd = 'SOMEMODIFIER' then tval_char end)... FROM observation_facts WHERE ...
      Maybe doing something with unnest() can also help but I haven't figured that out yet.
    For Oracle we should look into the UNPIVOT operator
     */

}

@TupleConstructor
class Query {
    DetachedCriteria<ObservationFact> criteria
    Map params
    List<Closure> projection
    List<Dimension> projectionOwners
}