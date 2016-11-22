
com.rwg.solr.scheme = 'http'
com.rwg.solr.host   = 'localhost:8983'
com.rwg.solr.facets.path = 'solr/facets/'
com.recomdata.FmFolderService.filestoreDirectory = (new File(System.getenv('HOME'), '.grails/transmart-filestore')).absolutePath

//grails.plugin.reveng.packageName = 'org.transmartproject.search.browse'
//grails.plugin.reveng.includeTables = ['folder_study_mapping']
//grails.plugin.reveng.defaultSchema = 'biomart_user'


hibernate {
    cache.use_second_level_cache = true
    cache.use_query_cache = false
//    cache.region.factory_class = 'net.sf.ehcache.hibernate.EhCacheRegionFactory' // Hibernate 3
    cache.region.factory_class = 'org.hibernate.cache.ehcache.EhCacheRegionFactory' // Hibernate 4
    singleSession = true // configure OSIV singleSession mode
}

// environment specific settings
environments {
    test {
        dataSource {
            driverClassName = 'org.postgresql.Driver'
            url = 'jdbc:postgresql://localhost:5432/transmart'
            username = 'biomart_user'
            password = 'biomart_user'
            dbCreate = 'none'
            logSql = true
            formatSql = true
        }
    }
}

/*
log4j = {
    // Example of changing the log pattern for the default console
    // appender:
    //
    //appenders {
    //    console name:'stdout', layout:pattern(conversionPattern: '%c{2} %m%n')
    //}

    error  'org.codehaus.groovy.grails.web.servlet',  //  controllers
            'org.codehaus.groovy.grails.web.pages', //  GSP
            'org.codehaus.groovy.grails.web.sitemesh', //  layouts
            'org.codehaus.groovy.grails.web.mapping.filter', // URL mapping
            'org.codehaus.groovy.grails.web.mapping', // URL mapping
            'org.codehaus.groovy.grails.commons', // core / classloading
            'org.codehaus.groovy.grails.plugins', // plugins
            'org.codehaus.groovy.grails.orm.hibernate', // hibernate integration
            'org.springframework',
            'org.hibernate',
            'net.sf.ehcache.hibernate'

    //trace 'org.hibernate.type'
    //debug 'org.hibernate.SQL'
    debug 'org.transmartproject.search.indexing'
}
*/
grails.cache.config = {
    cache {
        name 'FacetsIndexCache'
        eternal false
        timeToLiveSeconds(15 * 60)
        maxElementsInMemory 10
        maxElementsOnDisk 0
    }
}