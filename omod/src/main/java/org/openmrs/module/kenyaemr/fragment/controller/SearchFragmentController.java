/**
 * The contents of this file are subject to the OpenMRS Public License
 * Version 1.0 (the "License"); you may not use this file except in
 * compliance with the License. You may obtain a copy of the License at
 * http://license.openmrs.org
 *
 * Software distributed under the License is distributed on an "AS IS"
 * basis, WITHOUT WARRANTY OF ANY KIND, either express or implied. See the
 * License for the specific language governing rights and limitations
 * under the License.
 *
 * Copyright (C) OpenMRS, LLC.  All Rights Reserved.
 */

package org.openmrs.module.kenyaemr.fragment.controller;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import javax.servlet.http.HttpSession;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.openmrs.Concept;
import org.openmrs.ConceptClass;
import org.openmrs.ConceptSearchResult;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.PatientIdentifierType;
import org.openmrs.Person;
import org.openmrs.Provider;
import org.openmrs.User;
import org.openmrs.Visit;
import org.openmrs.api.ConceptService;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.calculation.patient.PatientCalculationContext;
import org.openmrs.calculation.patient.PatientCalculationService;
import org.openmrs.calculation.result.CalculationResultMap;
import org.openmrs.calculation.result.ListResult;
import org.openmrs.module.kenyacore.CoreConstants;
import org.openmrs.module.kenyacore.calculation.CalculationUtils;
import org.openmrs.module.kenyaemr.CommonUtils;
import org.openmrs.module.kenyaemr.EmrWebConstants;
import org.openmrs.module.kenyaemr.api.KenyaEmrService;
import org.openmrs.module.kenyaemr.calculation.library.ScheduledVisitOnDayCalculation;
import org.openmrs.module.kenyaemr.calculation.library.VisitsOnDayCalculation;
import org.openmrs.ui.framework.SimpleObject;
import org.openmrs.ui.framework.UiUtils;
import org.openmrs.util.OpenmrsConstants;
import org.openmrs.util.PersonByNameComparator;
import org.openmrs.web.user.CurrentUsers;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Fragment actions specifically for searching for OpenMRS objects
 */
public class SearchFragmentController {

	protected static final Log log = LogFactory.getLog(SearchFragmentController.class);

	/**
	 * Gets a patient by their id
	 * @param patient the patient
	 * @param ui the UI utils
	 * @return the simplified patient
	 */
	public SimpleObject patient(@RequestParam("id") Patient patient, UiUtils ui) {
		SimpleObject ret = ui.simplifyObject(patient);

		// Simplify and attach active visit to patient object
		List<Visit> activeVisits = Context.getVisitService().getActiveVisitsByPatient(patient);
		ret.put("activeVisit", activeVisits.size() > 0 ? ui.simplifyObject(activeVisits.get(0)) : null);
		return ret;
	}

	/**
	 * Searches for patients by name, identifier, age, visit status
	 * @param query the name or identifier
	 * @param which all|checked-in|non-accounts
	 * @param ui the UI utils
	 * @return the simple patients
	 */
	public List<SimpleObject> patients(@RequestParam(value = "q", required = false) String query,
									   @RequestParam(value = "which", required = false, defaultValue = "all") String which,
									   UiUtils ui) {

		log.error("search normal");
		// Return empty list if we don't have enough input to search on
		if (StringUtils.isBlank(query) && "all".equals(which)) {
			return Collections.emptyList();
		}

		// Run main patient search query based on id/name
		List<Patient> matchedByNameOrID = Context.getPatientService().getPatients(query);

		// Gather up active visits for all patients. These are attached to the returned patient representations.
		Map<Patient, Visit> patientActiveVisits = getActiveVisitsByPatients();

		List<Patient> matched = new ArrayList<Patient>();

		// If query wasn't long enough to be searched on, and they've requested checked-in patients, return the list
		// of checked in patients
		if (StringUtils.isBlank(query) && "checked-in".equals(which)) {
			matched.addAll(patientActiveVisits.keySet());
			Collections.sort(matched, new PersonByNameComparator()); // Sort by person name
		}
		else {
			if ("all".equals(which)) {
				matched = matchedByNameOrID;
			}
			else if ("checked-in".equals(which)) {
				for (Patient patient : matchedByNameOrID) {
					if (patientActiveVisits.containsKey(patient)) {
						matched.add(patient);
					}
				}
			}
			else if ("non-accounts".equals(which)) {
				Set<Person> accounts = new HashSet<Person>();
				accounts.addAll(getUsersByPersons(query).keySet());
				accounts.addAll(getProvidersByPersons(query).keySet());

				for (Patient patient : matchedByNameOrID) {
					if (!accounts.contains(patient)) {
						matched.add(patient);
					}
				}
			}
		}

		// Simplify and attach active visits to patient objects
		List<SimpleObject> simplePatients = new ArrayList<SimpleObject>();
		for (Patient patient : matched) {
			SimpleObject simplePatient = ui.simplifyObject(patient);

			Visit activeVisit = patientActiveVisits.get(patient);
			simplePatient.put("activeVisit", activeVisit != null ? ui.simplifyObject(activeVisit) : null);
			simplePatient.put("patientName", patient.getGivenName());
			
			simplePatients.add(simplePatient);
		}

		return simplePatients;
	}
	
	private  Date parseDate(String s) throws ParseException {
		DateFormat df = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
		if (s == null || s.length() == 0) {
			return null;
		} else {
			if (s.length() == 10) {
				s += " 00:00:00";
			}
			return df.parse(s);
		}
	}
	
	
	/**
	 * Searches for patients by name, identifier, age, visit status
	 * @param query the name or identifier
	 * @param which all|checked-in|non-accounts
	 * @param ui the UI utils
	 * @return the simple patients
	 */
	public List<SimpleObject> patientsWithDate(@RequestParam(value = "date", required = false) String date,
										@RequestParam(value = "q", required = false) String query,
									   @RequestParam(value = "which", required = false, defaultValue = "all") String which,
									   UiUtils ui) {
//		log.error("info search patient query: " +query);
		Date scheduledDate = null;
		try {
			scheduledDate = parseDate(date);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}

		// Return empty list if we don't have enough input to search on
		if (StringUtils.isBlank(query) && "all".equals(which) && StringUtils.isBlank(date)) {
			return Collections.emptyList();
		}

		// Run main patient search query based on id/name
		List<Patient> matchedByNameOrID = Context.getPatientService().getPatients(query);
		List<Patient> matchedByID  = Context.getPatientService().getPatients(null, query, null, true);
		matchedByNameOrID.addAll(matchedByID);
		
		// Gather up active visits for all patients. These are attached to the returned patient representations.
		Map<Patient, Visit> patientActiveVisits = getActiveVisitsByPatients();

		List<Patient> matched = new ArrayList<Patient>();

		// If query wasn't long enough to be searched on, and they've requested checked-in patients, return the list
		// of checked in patients
		if (StringUtils.isBlank(query) && "checked-in".equals(which)) {
			matched.addAll(patientActiveVisits.keySet());
			Collections.sort(matched, new PersonByNameComparator()); // Sort by person name
		}
		else {
			if ("all".equals(which)) {
				matched = matchedByNameOrID;
			}
			else if ("checked-in".equals(which)) {
				for (Patient patient : matchedByNameOrID) {
					if (patientActiveVisits.containsKey(patient)) {
						matched.add(patient);
					}
				}
			}
			else if ("non-accounts".equals(which)) {
				Set<Person> accounts = new HashSet<Person>();
				accounts.addAll(getUsersByPersons(query).keySet());
				accounts.addAll(getProvidersByPersons(query).keySet());

				for (Patient patient : matchedByNameOrID) {
					if (!accounts.contains(patient)) {
						matched.add(patient);
					}
				}
			}
		}
		
		List<SimpleObject> simplePatients = new ArrayList<SimpleObject>();

		// query by scheduled date
		if (StringUtils.isNotEmpty(date)) {
			PatientCalculationService cs = Context.getService(PatientCalculationService.class);
			Set<Integer> allPatients = new HashSet<Integer>();
			if (!matched.isEmpty()) {
				for (Patient p : matched) {
					allPatients.add(p.getPatientId());
				}
			} else {
				allPatients = Context.getPatientSetService().getAllPatients().getMemberIds();
			}
			
	
	
			Map<String, Object> params = new HashMap<String, Object>();
	
			params.put("date", scheduledDate);
	
			PatientCalculationContext calcContext = cs.createCalculationContext();
	
	
			Set<Integer> scheduled = CalculationUtils.patientsThatPass(cs.evaluate(allPatients, new ScheduledVisitOnDayCalculation(), params, calcContext));
	
	
			CalculationResultMap actual = cs.evaluate(scheduled, new VisitsOnDayCalculation(), params, calcContext);
			
			// Sort patients and convert to simple objects
			List<Patient> scheduledPatients = Context.getPatientSetService().getPatients(scheduled);
	
			Collections.sort(scheduledPatients, new PersonByNameComparator());
	
	
			simplePatients = new ArrayList<SimpleObject>();
	
			for (Patient p : scheduledPatients) {
	
				SimpleObject so = ui.simplifyObject(p);
		
				ListResult visitsResult = (ListResult) actual.get(p.getPatientId());
		
				List<Visit> visits = CalculationUtils.extractResultValues(visitsResult);
		
				so.put("visits", ui.simplifyCollection(visits));
		
				simplePatients.add(so);
		
	
			}
		} else {
			// Simplify and attach active visits to patient objects
			for (Patient patient : matched) {
				SimpleObject simplePatient = ui.simplifyObject(patient);
				List<Visit> visits = Context.getVisitService().getActiveVisitsByPatient(patient);
				for(Visit v : visits) {
					if(v.getVisitType().getName().equalsIgnoreCase(EmrWebConstants.VISIT_TYPE_NEW_PATIENT)){
						simplePatient.put("newVisit", "true");
						break;
					}
				}
				Visit activeVisit = patientActiveVisits.get(patient);
				simplePatient.put("activeVisit",ui.simplifyObject(activeVisit));
				simplePatient.put("patientName", patient.getGivenName());
				simplePatients.add(simplePatient);
			}
		}


		
		


		return simplePatients;
	}

	/**
	 * Gets a location by it's id
	 * @param location the location
	 * @param ui the UI utils
	 * @return the simplified location
	 */
	public SimpleObject location(@RequestParam("id") Location location, UiUtils ui) {
		return ui.simplifyObject(location);
	}

	/**
	 * Searches for locations by name or MFL code
	 * @param query the search query
	 * @param ui the UI utils
	 * @return the simplified locations
	 */
	public SimpleObject[] locations(@RequestParam("q") String query, UiUtils ui) {
		LocationService svc = Context.getLocationService();

		// Results will be sorted by name
		Set<Location> results = new TreeSet<Location>(new Comparator<Location>() {
			@Override
			public int compare(Location location1, Location location2) {
			return location1.getName().compareTo(location2.getName());
			}
		});

		// If term looks like an MFL code, add location with that code
		if (StringUtils.isNumeric(query) && query.length() >= 5) {
			Location locationByMflCode = Context.getService(KenyaEmrService.class).getLocationByMflCode(query);
			if (locationByMflCode != null) {
				results.add(locationByMflCode);
			}
		}

		// Add first 20 results of search by name
		if (StringUtils.isNotBlank(query)) {
			results.addAll(svc.getLocations(query, true, 0, 20));
		}

		// Convert to simple objects
		return ui.simplifyCollection(results);
	}

	/**
	 * Gets a person by their id
	 * @param person the person
	 * @param ui the UI utils
	 * @return the simplified person
	 */
	public SimpleObject person(@RequestParam("id") Person person, UiUtils ui) {
		return ui.simplifyObject(person);
	}

	/**
	 * Searches for persons by name
	 * @param query the name query
	 * @param ui the UI utils
	 * @return the simplified persons
	 */
	public SimpleObject[] persons(@RequestParam(value = "q", required = false) String query, UiUtils ui) {
		Collection<Person> results = Context.getPersonService().getPeople(query, null);

		// Convert to simple objects
		return ui.simplifyCollection(results);
	}

	/**
	 * Gets a provider by their id
	 * @param provider the provider
	 * @param ui the UI utils
	 * @return the simplified provider
	 */
	public SimpleObject provider(@RequestParam("id") Provider provider, UiUtils ui) {
		return ui.simplifyObject(provider);
	}

	/**
	 * Searches for providers by name
	 * @param query the name query
	 * @param ui the UI utils
	 * @return the simplified providers
	 */
	public SimpleObject[] providers(@RequestParam(value = "q", required = false) String query, UiUtils ui) {
		Collection<Provider> results = Context.getProviderService().getProviders(query, null, null, null);

		// Convert to simple objects
		return ui.simplifyCollection(results);
	}

	/**
	 * Searches for accounts by name
	 * @param query the name query
	 * @param which all|providers|users|non-patients
	 * @param ui
	 * @return
	 */
	public List<SimpleObject> accounts(@RequestParam(value = "q", required = false) String query,
									   @RequestParam(value = "which", required = false, defaultValue = "all") String which,
									   HttpSession session,
									   UiUtils ui) {

		Map<Person, User> userAccounts = new HashMap<Person, User>();
		Map<Person, Provider> providerAccounts = new HashMap<Person, Provider>();

		if (!"providers".equals(which)) {
			userAccounts = getUsersByPersons(query);
		}

		if (!"users".equals(which)) {
			providerAccounts = getProvidersByPersons(query);
		}

		Set<Person> persons = new TreeSet<Person>(new PersonByNameComparator());
		persons.addAll(userAccounts.keySet());
		persons.addAll(providerAccounts.keySet());

		Set<String> onlineUsers = new HashSet<String>(CurrentUsers.getCurrentUsernames(session));

		List<SimpleObject> ret = new ArrayList<SimpleObject>();
		for (Person p : persons) {
			if ("non-patients".equals(which) && p.isPatient()) {
				continue;
			}

			// Simplify person first
			SimpleObject account = ui.simplifyObject(p);

			User user = userAccounts.get(p);
			if (user != null) {
				boolean online;
				String username = user.getUsername();

				// Admin account doesn't have a username
				if (StringUtils.isBlank(username)) {
					online = onlineUsers.contains("systemid:" + user.getSystemId());
					username =  user.getSystemId();
				}
				else {
					online = onlineUsers.contains(username);
				}

				account.put("user", SimpleObject.create("id", user.getId(), "username", username, "online", online));
			}

			Provider provider = providerAccounts.get(p);
			if (provider != null) {
				account.put("provider", SimpleObject.fromObject(provider, ui, "identifier"));
			}

			ret.add(account);
		}

		return ret;
	}

	/**
	 * Gets a concept by it's id
	 * @param concept the concept
	 * @param ui the UI utils
	 * @return the simplified concept
	 */
	public SimpleObject concept(@RequestParam("id") Concept concept, UiUtils ui) {
		return ui.simplifyObject(concept);
	}

	/**
	 * Searches for concept by name and class
	 * @param query the name query
	 * @param conceptClass the concept class
	 * @param ui the UI utils
	 * @return the simplified concepts
	 */
	public List<SimpleObject> concepts(@RequestParam(value = "q", required = false) String query,
									   @RequestParam(value = "class", required = false) ConceptClass conceptClass,
									   @RequestParam(value = "answerTo", required = false) Concept answerTo,
									   @RequestParam(value = "size", required = false) Integer size,
									   UiUtils ui) {

		ConceptService conceptService = Context.getConceptService();

		List<ConceptClass> conceptClasses = conceptClass != null ? Collections.singletonList(conceptClass) : null;

		List<ConceptSearchResult> results = conceptService.getConcepts(query, Collections.singletonList(CoreConstants.LOCALE), false,
				conceptClasses, null, null, null, answerTo, 0, size);

		// Simplify results
		List<SimpleObject> simpleConcepts = new ArrayList<SimpleObject>();
		for (ConceptSearchResult result : results) {
			simpleConcepts.add(ui.simplifyObject(result.getConcept()));
		}

		return simpleConcepts;
	}

	/**
	 * Helper method to get all active visits organised by patient
	 * @return the map of patients to active visits
	 */
	protected Map<Patient, Visit> getActiveVisitsByPatients() {
		List<Visit> activeVisits = Context.getVisitService().getVisits(null, null, null, null, null, null, null, null, null, false, false);
		Map<Patient, Visit> patientToVisits = new HashMap<Patient, Visit>();
		for (Visit visit : activeVisits) {
			patientToVisits.put(visit.getPatient(), visit);
		}
		return patientToVisits;
	}

	/**
	 * Helper method to get users organised by person
	 * @param query the name query
	 * @return the map of persons to users
	 */
	protected Map<Person, User> getUsersByPersons(String query) {
		Map<Person, User> personToUsers = new HashMap<Person, User>();
		for (User user : Context.getUserService().getUsers(query, null, true)) {
			if (!"daemon".equals(user.getUsername())) {
				personToUsers.put(user.getPerson(), user);
			}
		}
		return personToUsers;
	}

	/**
	 * Helper method to get all providers organised by person
	 * @param query the name query
	 * @return the map of persons to providers
	 */
	protected Map<Person, Provider> getProvidersByPersons(String query) {
		Map<Person, Provider> personToProviders = new HashMap<Person, Provider>();
		List<Provider> providers = Context.getProviderService().getProviders(query, null, null, null);
		for (Provider p : providers) {
			if (p.getPerson() != null) {
				personToProviders.put(p.getPerson(), p);
			}
		}
		return personToProviders;
	}

	/**
	 * Gets the minimum number of query characters required for a service search method
	 * @return the value of min search characters
	 */
	protected static int getMinSearchCharacters() {
		int minSearchCharacters = OpenmrsConstants.GLOBAL_PROPERTY_DEFAULT_MIN_SEARCH_CHARACTERS;
		String minSearchCharactersStr = Context.getAdministrationService().getGlobalProperty(OpenmrsConstants.GLOBAL_PROPERTY_MIN_SEARCH_CHARACTERS);

		try {
			minSearchCharacters = Integer.valueOf(minSearchCharactersStr);
		}
		catch (NumberFormatException e) {
			//do nothing
		}
		return minSearchCharacters;
	}
}