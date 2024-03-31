package manager;
import java.util.ArrayList;
import java.util.List;


import org.hibernate.HibernateException;
import org.hibernate.Session;
import org.hibernate.Transaction;
import org.hibernate.cfg.Configuration;
import org.hibernate.query.Query;

import model.Bodega;
import model.Campo;
import model.Entrada;
import model.Vid;
import utils.TipoVid;

import java.util.ArrayList;
import java.util.List;

public class Manager {
    private static Manager manager;
    private List<Entrada> entradas;
    private List<Campo> campos;
    private Session session;
    private Transaction tx;
    private Bodega b;

    private Manager() {
        this.entradas = new ArrayList<>();
        this.campos = new ArrayList<>();
    }

    public static Manager getInstance() {
        if (manager == null) {
            manager = new Manager();
        }
        return manager;
    }

    public void init() {
        createSession();
        getEntradas();
        manageActions();
        showAllCampos();
        showMaxBodega();
        session.close();
    }

    private void createSession() {
        org.hibernate.SessionFactory sessionFactory = new Configuration().configure().buildSessionFactory();
        session = sessionFactory.openSession();
    }

    private void manageActions() {
        for (Entrada entrada : entradas) {
            try {
                System.out.println(entrada.getInstruccion());
                switch (entrada.getInstruccion().toUpperCase().split(" ")[0]) {
                    case "B":
                        addBodega(entrada.getInstruccion().split(" "));
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
                if (tx != null) {
                    tx.rollback();
                }
            }
        }
    }

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
        Campo c = campos.get(campos.size() - 1); // Get the latest added Campo
        c.addVid(v);

        tx = session.beginTransaction();
        session.save(v);
        session.save(c);
        tx.commit();
    }

    private void addCampo(String[] split) {
        if (b == null) {
            System.out.println("No hay bodega registrada para agregar campo.");
            return;
        }

        Campo c = new Campo(b);
        campos.add(c); // Add the Campo to the list

        tx = session.beginTransaction();
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
    }
}
