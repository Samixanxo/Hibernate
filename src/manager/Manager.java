package manager;
import java.util.ArrayList;

import java.util.List;

import org.bson.Document;
import org.bson.conversions.Bson;
import org.hibernate.HibernateException;

import com.mongodb.MongoClient;
import com.mongodb.MongoClientURI;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Sorts;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.UpdateResult;
import model.Bodega;
import model.Campo;
import model.Entrada;
import model.Vid;

public class Manager {
    private static Manager manager;
    private List<Entrada> entradas;
    private List<Campo> campos;
    //private Session session;
    //private Transaction tx;
    private Bodega b;
    MongoDatabase database;
    MongoCollection<Document> collection;
    private ArrayList<Entrada> inputs;

    private Manager() {
        this.entradas = new ArrayList<>();
        this.campos = new ArrayList<>();
        this.inputs = new ArrayList<>();
    }

    public static Manager getInstance() {
        if (manager == null) {
            manager = new Manager();
        }
        return manager;
    }

    public void init() {
        createSession();
        getInputData();
        manageActions();
        //addWinery();
        //getEntradas();
        //manageActions();
        //showAllCampos();
        //showMaxBodega();
        //session.close();
    }

    private void createSession() {
        String uri = "mongodb://localhost:27017";
        MongoClientURI mongoClientUri = new MongoClientURI(uri);
        MongoClient mongoClient = new MongoClient(mongoClientUri);
        database = mongoClient.getDatabase("viticulture");
    }

    



    private void manageActions() {
        for (Entrada entrada : inputs) {
            try {
                System.out.println(entrada.getInstruccion());
                switch (entrada.getInstruccion().toUpperCase().split(" ")[0]) {
                    case "B":
                    	addWinery(entrada.getInstruccion().split(" "));
                        break;
                    case "C":
                        addCampo(entrada.getInstruccion().split(" "));
                        break;
                    case "V":
                        addVid(entrada.getInstruccion().split(" "));
                        break;
                    case "#":
                        vendimia();
                        break;
                    default:
                        System.out.println("Instrucción incorrecta");
                }
            } catch (HibernateException e) {
                e.printStackTrace();
               System.out.println("Valiste burguer");
            }
        }
        
    }
    
    
    public ArrayList<Entrada> getInputData() {
        MongoCollection<Document> collection = database.getCollection("entrada");

        for (Document document : collection.find()) {
            Entrada input = new Entrada();
            input.setId(document.getObjectId("_id").toString());
            input.setInstruccion(document.getString("instruccion"));
            inputs.add(input);
        }
        System.out.println(inputs);
        return inputs;
    }

    public void addWinery(String[] parts) {
        if (parts.length >= 2) {
            String nombre = parts[1];
            Bodega bodega = new Bodega();
            bodega.setName(nombre);
            collection = database.getCollection("bodega");
            Document document = new Document();
            document.put("name", bodega.getName());
            collection.insertOne(document);
        } else {
            System.out.println("La instrucción no tiene el formato esperado.");
        }
    }
    
    private void addCampo(String[] split) {
        try {
            String lastBodegaId = getLastBodegaId(); 
            Document document = new Document();
            collection = database.getCollection("campo");
            collection.insertOne(document);
            System.out.println("Campo agregado correctamente.");
            String nuevoCampoId = document.getObjectId("_id").toString();
            Campo nuevoCampo = new Campo();
            nuevoCampo.setId(nuevoCampoId);
            campos.add(nuevoCampo);
        } catch (Exception e) {
            System.out.println("Error al agregar el campo: " + e.getMessage());
            e.printStackTrace();
        }
    }


    private String getLastBodegaId() {
        MongoCollection<Document> bodegaCollection = database.getCollection("bodega");  
        Document lastBodega = bodegaCollection.find().sort(Sorts.descending("_id")).first();
        if (lastBodega != null) {
           
            String lastBodegaId = lastBodega.getObjectId("_id").toString();
            return lastBodegaId;
        } else {
            return null; 
        }
    }
    
    private String getLastCampoId() {
        MongoCollection<Document> campoCollection = database.getCollection("campo");
        Document lastCampo = campoCollection.find().sort(Sorts.descending("_id")).first();
        if (lastCampo != null) {
            String lastCampoId = lastCampo.getObjectId("_id").toString();
            return lastCampoId;
        } else {
            return null;
        }
    }

    
    
    private void addVid(String[] split) {
        if (campos.isEmpty()) {
            System.out.println("No hay campos registrados para agregar vid.");
            return;
        }

        String tipoVidStr = split[1].toLowerCase();
        int tipoVid;
        if (tipoVidStr.equals("blanca")) {
            tipoVid = 0;
        } else if (tipoVidStr.equals("negra")) {
            tipoVid = 1;
        } else {
            System.out.println("Tipo de vid no válido.");
            return;
        }
        int cantidad = Integer.parseInt(split[2]);      
        String campoId = getLastCampoId();       
        String bodegaId = getLastBodegaId();

        Document vidDocument = new Document("tipo_vid", tipoVid)
                                        .append("cantidad", cantidad)
                                        .append("campo_id", campoId)
                                        .append("bodegaId", bodegaId); 

        MongoCollection<Document> vidCollection = database.getCollection("vid");
        vidCollection.insertOne(vidDocument);
        System.out.println("Documento de vid agregado correctamente a la colección 'vid'.");
    }


    private void vendimia() {
        String bodegaId = getLastBodegaId();
        MongoCollection<Document> vidCollection = database.getCollection("vid");
        Bson filter = new Document("bodegaId", new Document("$exists", false)); // Solo actualiza los documentos que no tienen el campo "bodegaId"
        Bson update = Updates.set("bodegaId", bodegaId);
        UpdateResult updateResult = vidCollection.updateMany(filter, update);
        System.out.println("Número de documentos de vid actualizados: " + updateResult.getModifiedCount());
    }
    
    
    /*

    private void vendimia() {
        for (Campo campo : campos) {
            this.b.getVids().addAll(campo.getVids());
            campo.recolected();
            tx = session.beginTransaction();
            session.save(campo);
            tx.commit();
        }

        tx = session.beginTransaction();
        session.save(b);
        tx.commit();
    }

    private void addVid(String[] split) {
        if (campos.isEmpty()) {
            System.out.println("No hay campos registrados para agregar vid.");
            return;
        }

        Vid v = new Vid(TipoVid.valueOf(split[1].toUpperCase()), Integer.parseInt(split[2]));
        Campo c = campos.get(campos.size() - 1); 
        c.addVid(v);

        tx = session.beginTransaction();
        session.save(v);
        session.save(c);
        tx.commit();
    }

    

    private void addBodega(String[] split) {
        b = new Bodega(split[1]);

        tx = session.beginTransaction();
        session.save(b);
        tx.commit();
    }

    private void getEntradas() {
        tx = session.beginTransaction();
        Query q = session.createQuery("select e from Entrada e");
        entradas.addAll(q.list());
        tx.commit();
    }

    private void showAllCampos() {
        tx = session.beginTransaction();
        Query q = session.createQuery("select c from Campo c");
        campos.addAll(q.list()); // Populate the campos list
        for (Campo c : campos) {
            System.out.println(c);
        }
        tx.commit();
    }

    private void showMaxBodega() {
        tx = session.beginTransaction();
        Query query = session.createQuery("FROM Bodega ORDER BY id DESC");
        query.setMaxResults(1);
        Bodega maxBodega = (Bodega) query.uniqueResult();
        System.out.println("Bodega con el ID más alto: " + maxBodega);
        tx.commit();
    }*/
}
