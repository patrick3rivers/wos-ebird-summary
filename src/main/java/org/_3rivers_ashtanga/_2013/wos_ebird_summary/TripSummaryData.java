package org._3rivers_ashtanga._2013.wos_ebird_summary;

class TripSummaryData {

    private final String species;
    private final String category;
    private final int count;

    TripSummaryData(String species, String category, int count) {
	this.species = species;
	this.category = category;
	this.count = count;
    }
    public String getSpecies() {
	return this.species ;
    }
    public String getCategory() {
	return this.category;
    }
    public int getCount() {
	return this.count;
    }

}
