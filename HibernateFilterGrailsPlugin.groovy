import org.codehaus.groovy.grails.commons.GrailsClass
import org.codehaus.groovy.grails.commons.GrailsDomainClass
import org.grails.plugin.hibernate.filter.DefaultHibernateFiltersHolder
import org.grails.plugin.hibernate.filter.HibernateFilterUtils

class HibernateFilterGrailsPlugin {
	def version = '0.2'
	def grailsVersion = '1.3 > *'
	def loadAfter = ['controllers', 'hibernate']
	def observe = ['*']
	def pluginExcludes = ['grails-app/domain/**']

	def author = 'Scott Burch'
	def authorEmail = 'scott@bulldoginfo.com'
	def title = 'Hibernate Filter plugin'
	def description = 'Integrates Hibernate filtering into Grails'
	def documentation = 'http://grails.org/plugin/hibernate-filter'

	def doWithDynamicMethods = { ctx ->
		for (GrailsDomainClass dc in application.domainClasses) {
			HibernateFilterUtils.addDomainClassMethods dc.clazz, ctx
		}

		for (Class artefactClass in application.allArtefacts) {
			HibernateFilterUtils.addDomainProxies artefactClass
		}
	}

	def onChange = { event ->

		HibernateFilterUtils.addDomainProxies event.source

		if (event.application.isDomainClass(event.source)) {
			HibernateFilterUtils.addDomainClassMethods event.source, event.ctx
		}
	}
}
