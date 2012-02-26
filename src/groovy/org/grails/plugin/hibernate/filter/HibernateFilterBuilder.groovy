package org.grails.plugin.hibernate.filter

import grails.util.Metadata

import org.apache.commons.lang.StringUtils
import org.codehaus.groovy.grails.commons.GrailsClass
import org.hibernate.cfg.Mappings
import org.hibernate.engine.FilterDefinition
import org.hibernate.type.TypeFactory
import org.slf4j.Logger
import org.slf4j.LoggerFactory

/**
 * Add the filters from the domain closure
 */
class HibernateFilterBuilder {

	private Logger log = LoggerFactory.getLogger(getClass())
	private boolean grails1 = Metadata.current.getGrailsVersion().startsWith('1')
	private Mappings mappings

	HibernateFilterDomainConfiguration configuration
	GrailsClass domainClass

	HibernateFilterBuilder(HibernateFilterDomainConfiguration configuration, GrailsClass domainClass) {
		this.configuration = configuration
		this.domainClass = domainClass
		mappings = configuration.createMappings()

		Closure filtersClosure = domainClass.getPropertyValue('hibernateFilters')
		filtersClosure.delegate = this
		filtersClosure.resolveStrategy = Closure.DELEGATE_ONLY
		filtersClosure()
	}

	def methodMissing(String name, args) {
		args = [name] + args.collect { it }
		def filterMethod = metaClass.getMetaMethod('addFilter', args.collect{it.getClass()} as Object[])
		if (filterMethod) {
			return filterMethod.invoke(this, args as Object[])
		}

		throw new HibernateFilterException(
			"Invalid arguments in hibernateFilters closure [class:${domainClass.name}, name:${name}]")
	}

	// Add a previously registered filter
	private void addFilter(String name, Map options = [:]) {
		// Use supplied condition if there is one, otherwise take the condition that is already part of the named filter
		String condition = options.condition ?:
			configuration.getFilterDefinitions().get(name).getDefaultFilterCondition()

		// for condition with parameter
		String[] paramTypes = options.types?.tokenize(',') as String[]

		// Don't add a filter definition twice - if it is not added already, create the filter
		if (!configuration.getFilterDefinitions().get(name)) {
			def paramsMap = [:]
			StringUtils.substringsBetween(condition, ':', '=').eachWithIndex { String param, int i ->
				def type = grails1 ?
					TypeFactory.basic(paramTypes[i]) :
					mappings.getTypeResolver().basic(paramTypes[i])
				paramsMap[param.trim()] = type
			}

			configuration.addFilterDefinition new FilterDefinition(name, condition, paramsMap)
		}

		// If this is a collection, add the filter to the collection, else add the condition to the base class
		def entity = options.collection ?
			configuration.getCollectionMapping("${domainClass.fullName}.${options.collection}") :
			configuration.getClassMapping(domainClass.fullName)

		// now add the filter to the class or collection
		entity.addFilter name, condition

		// TODO: may be able to refactor this so that the factory creates the session with the filters rather than enabling them on each request
		if (options.default) {
			if (options.default instanceof Closure) {
				DefaultHibernateFiltersHolder.addDefaultFilterCallback name, options.default
			}
			else {
				DefaultHibernateFiltersHolder.addDefaultFilter name
			}
		}

		// store any domain alias proxies to be injected later
		if (options.aliasDomain) {
			DefaultHibernateFiltersHolder.addDomainAliasProxy(
				new HibernateFilterDomainProxy(domainClass.reference.wrappedInstance, options.aliasDomain, name))
		}
	}
}
