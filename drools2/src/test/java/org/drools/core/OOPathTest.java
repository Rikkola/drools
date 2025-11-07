package org.drools.core;

import org.drools.core.PathNode.ListPathNode;
import org.drools.core.PathNode.RootPathNode;
import org.drools.core.function.Tuple.Tuple1;
import org.drools.core.function.Tuple.Tuple2;
import org.drools.core.function.Tuple.Tuple3;
import org.drools.core.function.Tuple.Tuple4;
import org.drools.core.function.Tuple.Tuple5;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

public class OOPathTest {
    record Library(String name, List<Room> rooms) {
        public String toString() {
            return "Library[name=" + name +"]";
        }
    }

    record Room(String name, List<Shelf> shelves) {
        public String toString() {
            return "Room[name=" + name +"]";
        }
    }

    record Shelf(String name, List<Book> books) {
        public String toString() {
            return "Shelf[name=" + name +"]";
        }
    }

    record Book(String title, List<Page> pages) {
        public String toString() {
            return "Book[name=" + title +"]";
        }
    }

    record Page(int number, String content) {
        public String toString() {
            return "Page[number=" + number +"]";
        }
    }

    @Test
    public void test() {
        Library l1 = createLibrary("l1");

        RootPathNode<Library, Tuple1<Library>> library = new RootPathNode<>( l -> true);

        ListPathNode<Library, Room, Tuple2<Library, Room>> room = new ListPathNode<>(AccessType.LIST, 1, l -> l.rooms(), r -> r.name() != null, library);

        ListPathNode<Room, Shelf, Tuple3<Library, Room, Shelf>> shelf = new ListPathNode<>(AccessType.LIST, 2, r -> r.shelves(), s -> s.name() != null, room);

        ListPathNode<Shelf, Book, Tuple4<Library, Room, Shelf, Book>> book = new ListPathNode<>(AccessType.LIST, 3, s -> s.books(), b -> b.title() != null, shelf);

        ListPathNode<Book, Page, Tuple5<Library, Room, Shelf, Book, Page>> page = new ListPathNode<>(AccessType.LIST, 4, b -> b.pages(), p -> p.number() >= 0, book);

        OOPath<Library, Page, Tuple5<Library, Room, Shelf, Book, Page>> path = new OOPath<>(page);

        Iterator<Page> it = path.iterator(l1);

        Page p;
        while ( it.hasNext() ) {
            p = it.next();
            System.out.println(p + ":" + path.getPathContext(it).getContext(0).getTuple());
        }
    }

    @Test
    public void test1() {
        Library l1 = createLibrary("l1");

        OOPathBuilder builder = new OOPathBuilder();

        OOPath<Library, Page, Tuple5<Library, Room, Shelf, Book, Page>> path =
        builder.<Library>path()
               .<Room>path(l -> l.rooms(), r -> r.name() != null)
               .<Shelf>path(r -> r.shelves(), s -> s.name() != null)
               .<Book>path(s -> s.books(), b -> b.title() != null)
               .<Page>path(b -> b.pages(), p -> p.number() >= 0).build();

        Iterator<Page> it = path.iterator(l1);

        Page p;
        while ( it.hasNext() ) {
            p = it.next();
            System.out.println(p + ":" + path.getPathContext(it).getContext(0).getTuple());
        }
    }

    public Library createLibrary(String name) {
        Library library = new Library(name, new ArrayList<>());
        createRooms(library);
        return library;
    }

    public void createRooms(Library library) {
        for (int i = 0; i < 2; i++) {
            Room r = new Room(library.name() + "_r" + i, new ArrayList<>());
            library.rooms().add(r);
            createShelves(library, r);
        }
    }

    public void createShelves(Library library, Room room) {
        for (int i = 0; i < 2; i++) {
            Shelf s = new Shelf(library.name() + "_" + room.name() + "_s" + i, new ArrayList<>());
            room.shelves().add(s);
            createBook(library, room, s);
        }
    }

    public void createBook(Library library, Room room, Shelf shelf) {
        for (int i = 0; i < 2; i++) {
            Book b = new Book(library.name() + "_" + room.name() + "_" + shelf.name + "_b" + i, new ArrayList<>());
            shelf.books().add(b);
            createPages(library, room, shelf, b);
        }
    }

    public void createPages(Library library, Room room, Shelf shelf, Book book) {
        for (int i = 0; i < 2; i++) {
            Page p = new Page(i, "content" + i);
            book.pages().add(p);
        }
    }

}
