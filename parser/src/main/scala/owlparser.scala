
import org.semanticweb.owlapi.apibinding.OWLManager
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat
import org.semanticweb.owlapi.rdf.rdfxml.renderer.OWLOntologyXMLNamespaceManager
import org.semanticweb.owlapi.search.EntitySearcher
import java.io.File
import java.util.stream.Collectors
import java.util.stream.Stream
import org.semanticweb.owlapi.formats.OWLXMLDocumentFormat
import org.semanticweb.owlapi.model._
import org.semanticweb.owlapi.rdf.rdfxml.renderer.OWLOntologyXMLNamespaceManager
import org.semanticweb.owlapi.search.EntitySearcher
import java.io.File
import java.util.stream.Collectors
import java.util.stream.Stream
import java.util.stream.{Stream, IntStream}

class owlparser {
  
        val man: OWLOntologyManager = OWLManager.createOWLOntologyManager()

        val filepath = "/home/hduser/Downloads/univ-bench.owl"
        val sep = System.getProperty("file.separator")
        val filename = filepath.substring(filepath.lastIndexOf(sep) + 1, filepath.length())
        println("Input : " + filename + " (" + filepath + ")\n")
        val file = new File(filepath)

        var ontology: OWLOntology = null
        try {
            //load an ontology from file
    
            ontology = man.loadOntologyFromOntologyDocument(file)
        } catch { case ex:OWLOntologyCreationException =>  {
            println("[Invalid IRI]\tUnable to load ontology.\n\tInput: " + filepath +"\n")
        }
        }

        var format: OWLDocumentFormat = null
        
        if (ontology != null) {
            // get the ontology format
            format = man.getOntologyFormat(ontology)
        }
        
        var owlxmlFormat: OWLXMLDocumentFormat = new OWLXMLDocumentFormat();
        
        if (format != null && format.isPrefixOWLDocumentFormat()) {
            // copy all the prefixes from OWL document format to OWL XML format
            owlxmlFormat.copyPrefixesFrom(format.asPrefixOWLDocumentFormat());
        }
        
        try {
            if (ontology != null) {
                // create an ontology IRI out of a physical URI
                // save the ontology in OWL XML format
                man.saveOntology(ontology, owlxmlFormat, IRI.create(file.toURI()));
            }
        } catch { case ex:OWLOntologyStorageException => {
            System.out.println("Unable to save ontology in XML format.");
        }
        }
        
         if(ontology != null) {

            /*// get a reference to a data factory from an OWLOntologyManager.*/
            var factory: OWLDataFactory = man.getOWLDataFactory();
            //Stream<OWLAxiom> axioms = null;
            var axioms: java.util.stream.Stream[OWLAxiom] = null; 
            axioms = ontology.axioms();


            if (axioms != null){
                System.out.println("\nLoaded ontology with " + ontology.getAxiomCount() + " axioms");
            }
            else{
                System.out.println("\nLoaded ontology contains zero axioms.");
            }
            /*
            prints the namespaces that were used in the file an ontology was loaded from.
             */
            var nsManager:OWLOntologyXMLNamespaceManager  = new OWLOntologyXMLNamespaceManager(ontology, owlxmlFormat);
            System.out.println("\n\nNamespaces that were used in the file an ontology was loaded from:");
            var prefix : String = null;
            for ( prefix <- nsManager.getPrefixes) {
                if (prefix.length() != 0) {
                    System.out.println(prefix + " --> " + nsManager.getNamespaceForPrefix(prefix));
                }
                else {
                    System.out.println("Default: " + nsManager.getDefaultNamespace());
                }
            }
            
            val classesInOntologyCount  = ontology.classesInSignature().collect(Collectors.toSet()).size()
            
            if (classesInOntologyCount>0){
            	
                System.out.println("\n\nNumber of classes in the loaded ontology: "+classesInOntologyCount+"\n");
                
                // get all classes in ontology signature
                for (OWLClass owlClass : ontology.classesInSignature().collect(Collectors.toSet())) {
                    
                    OWLDeclarationAxiom declaration = factory.getOWLDeclarationAxiom(owlClass);
                    System.out.println(declaration);
                    Stream<OWLAnnotation> annotation = EntitySearcher.getAnnotations(owlClass.getIRI(), ontology);
                    annotation.forEach(System.out::println);

                    // get all axioms for each class
                    for (OWLAxiom owlClassAxiom : ontology.axioms(owlClass).collect(Collectors.toSet())) {
                        // create an object visitor to get to the subClass restrictions

                        /*Stream<OWLSubClassOfAxiom> allSubClassAxioms = ontology.subClassAxiomsForSuperClass(owlClass);
                        System.out.println("Printing SubClasses");
                        allSubClassAxioms.forEach(System.out::println);*/


                        owlClassAxiom.accept(new OWLObjectVisitor() {

                            // found the subClassOf axiom
                            public void visit(OWLSubClassOfAxiom subClassAxiom) {

                                if (subClassAxiom.getSuperClass() instanceof OWLClass && subClassAxiom.getSubClass()
                                		instanceof OWLClass) 
                                {
                                    System.out.println(subClassAxiom.toString());
                                }
                                // create an object visitor to read the underlying (subClassOf) restrictions
                                subClassAxiom.getSuperClass().accept(new OWLObjectVisitor() {

                                    public void visit(OWLObjectSomeValuesFrom someValuesFromAxiom) {
                                        System.out.println( subClassAxiom.toString() );
                                        System.out.println( someValuesFromAxiom.getClassExpressionType().toString() );
                                        System.out.println( someValuesFromAxiom.getProperty().toString() );
                                        System.out.println( someValuesFromAxiom.getFiller().toString() );
                                    }

                                    public void visit(OWLObjectExactCardinality exactCardinalityAxiom) {
                                        System.out.println(subClassAxiom.toString() );
                                        System.out.println(exactCardinalityAxiom.getClassExpressionType().toString() );
                                        System.out.println(exactCardinalityAxiom.getCardinality() );
                                        System.out.println( exactCardinalityAxiom.getProperty().toString() );
                                        System.out.println(exactCardinalityAxiom.getFiller().toString() );
                                    }

                                    public void visit(OWLObjectMinCardinality minCardinalityAxiom) {
                                        System.out.println( subClassAxiom.toString() );
                                        System.out.println(  minCardinalityAxiom.getClassExpressionType().toString() );
                                        System.out.println(  minCardinalityAxiom.getCardinality() );
                                        System.out.println(  minCardinalityAxiom.getProperty().toString() );
                                        System.out.println(  minCardinalityAxiom.getFiller().toString() );
                                    }

                                    public void visit(OWLObjectMaxCardinality maxCardinalityAxiom) {
                                        System.out.println(  subClassAxiom.toString() );
                                        System.out.println(  maxCardinalityAxiom.getClassExpressionType().toString() );
                                        System.out.println(  maxCardinalityAxiom.getCardinality() );
                                        System.out.println(  maxCardinalityAxiom.getProperty().toString() );
                                        System.out.println(  maxCardinalityAxiom.getFiller().toString() );
                                    }

                                })
                            }
                        })

                    }

                    System.out.println();
                }
            }
            else {
                System.out.println("There are no classes in the loaded ontology\n");
            }

        
    


}
          
}