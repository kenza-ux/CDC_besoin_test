package Batiment;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class MoteurOuverture {

    private Map<ILecteur, IPorte> assosciation = new HashMap();
    private List<IPorte> portesOuvertes = new ArrayList<>();
    private List<Badge> badgesBloque= new ArrayList<>();
    private int numBadgePasse;
    private Map<Porteur, LocalDate> assoBlocage_Porteur_Porte= new HashMap();
    private Map<Porteur,List<LocalDate>> assoPorteurDateBloc = new HashMap(); 
    private Map<Porteur, Map<IPorte, LocalDate>> blocage_Porteur_Porte_Date= new HashMap<>();
    private Map<Badge,Integer> badgePresentéSansPorteur = new HashMap();
    private boolean alarm=false;
    private LocalDate timeMaintenant= LocalDate.now(
    		);

    public MoteurOuverture() {

    }

    public void associer(IPorte p, ILecteur l) {
        assosciation.put(l, p);

    }


    public void interroger() {
        for (Map.Entry<ILecteur, IPorte> entry : assosciation.entrySet()) {	
        	
            Badge interm = entry.getKey().badgeDétécté();
            //on debloque la personne si il figure dans la list des personne bloque pour une date et on est pas dans cette derniere 
          if(interm!=null)  debloquerPersonneSiDateBlocageDifferente(interm);
            if (interm!=null && interm.getPersonne() != null ) {//feature de gestion de blocage selon porteur associé ou pas
                if (!badgesBloque.contains(interm)) {

                    this.numBadgePasse = interm.getNumSerie(); // recup le num du badge qui est passé
                    var porte= entry.getValue();

                    if (!portesOuvertes.contains(porte) && !isBloque(interm.getPersonne(), porte, timeMaintenant)) {

                        porte.ouvrir();
                        portesOuvertes.add(entry.getValue());
                    }
                }//else ça n'ouvre rien
            }     else {
            	Integer nombreFoisBadgeSansPorteurPresenté = badgePresentéSansPorteur.get(interm);
            	if(nombreFoisBadgeSansPorteurPresenté==null) {
            		badgePresentéSansPorteur.put(interm, new Integer(0));
            		 nombreFoisBadgeSansPorteurPresenté = 0;
            	}
            	nombreFoisBadgeSansPorteurPresenté=new Integer(nombreFoisBadgeSansPorteurPresenté.intValue()+1);
            	badgePresentéSansPorteur.replace(interm, nombreFoisBadgeSansPorteurPresenté);
            	// Vérifier si la valeur est supérieure ou égale à 3
            	if (nombreFoisBadgeSansPorteurPresenté >= 3) {
            	    this.alarm = true;
            	}
            }
        }
    }
    
    public void debloquerPersonneSiDateBlocageDifferente(Badge badge) {
    	Porteur porteur = badge.getPersonne();
    	if(porteur!=null) {
    		  if(this.assoPorteurDateBloc.keySet().contains(porteur) 
    	        		&& !this.assoPorteurDateBloc.get(porteur).contains(this.timeMaintenant)
    	        		&& !this.assoPorteurDateBloc.get(porteur).contains(LocalDate.of(3000, 01, 01)) 
    	        		&& this.badgesBloque.contains(badge)) this.débloquerBadge(badge);
    	    }
    	}
      



    public void blocPorteAccessPorteur(Porteur p,LocalDate ...dates){
    	List<LocalDate> datesList=new ArrayList<>();
    	for(LocalDate d : dates) {
    		datesList.add(d);
    	}
    	  if(this.assoPorteurDateBloc.keySet().contains(p)) {
    		  this.assoPorteurDateBloc.get(p).addAll(List.of(dates));
    	  }else {
    		  this.assoPorteurDateBloc.put(p,List.of(dates) );
    	  }
    	for(LocalDate d:dates) {
    	
    	  if(this.timeMaintenant.equals(d)){
    	          for (Badge b:p.getBadges()){
    	              bloquerBadge(b,datesList);
    	              
    	          }
    	      }
    	}
       
    }
    
    public void blocAccessDurant(Porteur p,LocalDate dateDebut,LocalDate dateFin) {
    	List<LocalDate> datesList=getDatesBetween(dateDebut,dateFin);
    	if(this.assoPorteurDateBloc.keySet().contains(p)) 
    	this.assoPorteurDateBloc.get(p).addAll(datesList);
  	    else
  	    this.assoPorteurDateBloc.put(p,datesList);
  	    for (Badge b:p.getBadges()){
  	         bloquerBadge(b,datesList);
  	        }
    }

    public void bloquerBadge(Badge b){
    	Porteur porteur = b.getPersonne();
    	if(this.assoPorteurDateBloc.containsKey(b.getPersonne())) this.assoPorteurDateBloc.replace(porteur, List.of(LocalDate.of(3000,01,01)));
    	else this.assoPorteurDateBloc.put(porteur,List.of(LocalDate.of(3000,01,01)));
       badgesBloque.add(b);
    }
    
    public void bloquerBadge(Badge b,List<LocalDate>dates){
    	Porteur porteur = b.getPersonne();
    	if(this.assoPorteurDateBloc.containsKey(b.getPersonne())) {
    		List<LocalDate> ds = this.assoPorteurDateBloc.get(porteur);
    		ds = new ArrayList<>();
    		ds.addAll(ds);
    		
    		}
    	else this.assoPorteurDateBloc.put(porteur,dates);
       badgesBloque.add(b);
    }
    
    public void bloquerPorteAccesPorteurJourPrecis(Porteur p, IPorte porte, LocalDate jourBlocage) {


        if (blocage_Porteur_Porte_Date.containsKey(p)) {
            blocage_Porteur_Porte_Date.replace(p, Map.of(porte, jourBlocage)); // Mettre à jour l'entrée existante
        } else {
            blocage_Porteur_Porte_Date.put(p, Map.of(porte, jourBlocage)); // Créer une nouvelle entrée
        }
    }
    
    private boolean isBloque(Porteur p, IPorte porte, LocalDate dateJour) {
        if (blocage_Porteur_Porte_Date.containsKey(p)) {
            Map<IPorte, LocalDate> blocagesPorteDate = blocage_Porteur_Porte_Date.get(p);
            return blocagesPorteDate.containsKey(porte) && blocagesPorteDate.get(porte).equals(dateJour);
        } else {
            return false;
        }
    }
    
    public boolean isAlarm() {
    	return this.alarm;
    }

    public void débloquerBadge(Badge b){
        badgesBloque.remove(b);
    }

    public int getNumBadgePasse() {
        return numBadgePasse;
    }

    public List<Badge> getBadgesBloque() {
        return badgesBloque;
    }

    public void setDateAujourdhui(LocalDate timeMaintenant) {
        this.timeMaintenant = timeMaintenant;
    }
    
    public List<LocalDate> getDatesBetween(LocalDate startDate, LocalDate endDate) {
        return Stream.iterate(startDate, date -> date.plusDays(1))
                     .limit(ChronoUnit.DAYS.between(startDate, endDate) + 1)
                     .collect(Collectors.toList());
    }

}