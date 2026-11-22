
package com.student;

import java.util.Scanner;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

public class App {

    static Scanner input = new Scanner(System.in);

    static Book[] books = new Book[100];
    static Book[] readingList = new Book[20];

    static int bookCount = 0;
    static int readingCount = 0;

    public static void main(String[] args) {

        int choice = -1;

        do {
            System.out.println("\n===== GUTENBERG BOOK EXPLORER =====");
            System.out.println("1. Explore Books");
            System.out.println("2. Search by Title");
            System.out.println("3. Search by Author");
            System.out.println("4. Add Book to Reading List");
            System.out.println("5. View Reading List");
            System.out.println("6. Remove Book");
            System.out.println("7. Display Book Details");
            System.out.println("0. Exit");

            System.out.print("Enter your choice: ");

            try {
                choice = Integer.parseInt(input.nextLine());

                switch (choice) {
                    case 1:
                        exploreBooks();
                        break;
                    case 2:
                        System.out.print("Enter book title: ");
                        searchBook(input.nextLine());
                        break;
                    case 3:
                        System.out.print("Enter author name: ");
                        searchBook(input.nextLine(), true);
                        break;
                    case 4:
                        addBook();
                        break;
                    case 5:
                        viewReadingList();
                        break;
                    case 6:
                        removeBook();
                        break;
                    case 7:
                        displayBookDetails();
                        break;
                    case 0:
                        System.out.println("Thank you for using Gutenberg Book Explorer!");
                        break;
                    default:
                        System.out.println("Invalid choice. Try again.");
                }

            } catch (NumberFormatException e) {
                System.out.println("Please enter a valid number.");
            }

        } while (choice != 0);
    }

    static void exploreBooks() {

        System.out.println("\nLoading books from Project Gutenberg...");

        if (loadBooks("https://www.gutenberg.org/browse/scores/top")) {
            displayBooks();
        }
    }

    // First overloaded search method
    static void searchBook(String title) {
        searchBook(title, false);
    }

    // Second overloaded search method
    static void searchBook(String keyword, boolean searchByAuthor) {

        if (keyword.trim().isEmpty()) {
            System.out.println("Search cannot be empty.");
            return;
        }

        try {
            String query = keyword;

            if (searchByAuthor) {
                query = "author:" + keyword;
            }

            String encoded = URLEncoder.encode(
                    query, StandardCharsets.UTF_8);

            String url = "https://www.gutenberg.org/ebooks/search/?query="
                    + encoded;

            System.out.println("\nSearching...");

            if (!loadBooks(url)) {
                System.out.println("No search results available.");
                return;
            }

            Book[] results = new Book[100];
            int count = 0;

            for (int i = 0; i < bookCount; i++) {

                boolean match;

                if (searchByAuthor) {
                    match = books[i].author.toLowerCase()
                            .contains(keyword.toLowerCase());
                } else {
                    match = books[i].title.toLowerCase()
                            .contains(keyword.toLowerCase());
                }

                if (match && count < results.length) {
                    results[count] = books[i];
                    count++;
                }
            }

            for (int i = 0; i < count; i++) {
                books[i] = results[i];
            }

            bookCount = count;

            if (bookCount == 0) {
                System.out.println("No matching books found.");
            } else {
                displayBooks();
            }

        } catch (Exception e) {
            System.out.println("Search error: " + e.getMessage());
        }
    }

    static boolean loadBooks(String url) {

        bookCount = 0;

        try {
            Document doc = Jsoup.connect(url)
                    .userAgent("Mozilla/5.0")
                    .header("Accept-Language", "en-US,en;q=0.9")
                    .timeout(20000)
                    .get();

            Elements entries = doc.select("li.booklink");

            for (Element entry : entries) {

                Element link = entry.selectFirst("a[href^=/ebooks/]");

                if (link == null) {
                    continue;
                }

                String bookUrl = link.absUrl("href");

                if (!bookUrl.matches(
                        "https://www\\.gutenberg\\.org/ebooks/\\d+/?")) {
                    continue;
                }

                Element titleElement = entry.selectFirst(".title");

                String title;

                if (titleElement != null) {
                    title = titleElement.text().trim();
                } else {
                    title = link.text().trim();
                }

                if (title.isEmpty()) {
                    continue;
                }

                String author = "Unknown Author";

                Element authorElement = entry.selectFirst(".subtitle");

                if (authorElement != null) {
                    author = authorElement.text().trim();
                }

                boolean duplicate = false;

                for (int i = 0; i < bookCount; i++) {
                    if (books[i].url.equals(bookUrl)) {
                        duplicate = true;
                        break;
                    }
                }

                if (!duplicate && bookCount < books.length) {
                    books[bookCount] = new Book(title, author, bookUrl);
                    bookCount++;
                }

                if (url.contains("/browse/scores/top")
                        && bookCount == 10) {
                    break;
                }
            }

            if (bookCount == 0) {
                System.out.println("No books found.");
                return false;
            }

            return true;

        } catch (Exception e) {
            System.out.println("Connection error: " + e.getMessage());
            return false;
        }
    }

    static void displayBooks() {

        if (bookCount == 0) {
            System.out.println("No books available.");
            return;
        }

        System.out.println("\n===== AVAILABLE BOOKS =====");

        for (int i = 0; i < bookCount; i++) {
            System.out.println("\nBook Number: " + (i + 1));
            System.out.println("Title: " + books[i].title);
            System.out.println("Author: " + books[i].author);
            System.out.println("URL: " + books[i].url);
        }

        System.out.println("\nTotal Books: " + bookCount);
    }

    static void addBook() {

        if (bookCount == 0) {
            System.out.println("Please explore or search for books first.");
            return;
        }

        if (readingCount == readingList.length) {
            System.out.println("Reading list is full.");
            return;
        }

        displayBooks();

        System.out.print("\nEnter book number to add: ");

        try {
            int number = Integer.parseInt(input.nextLine());

            if (number < 1 || number > bookCount) {
                System.out.println("Invalid book number.");
                return;
            }

            Book selectedBook = books[number - 1];

            for (int i = 0; i < readingCount; i++) {
                if (readingList[i].url.equals(selectedBook.url)) {
                    System.out.println("Book already exists in your list.");
                    return;
                }
            }

            readingList[readingCount] = selectedBook;
            readingCount++;

            System.out.println("Book added successfully!");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    static void viewReadingList() {

        System.out.println("\n===== MY READING LIST =====");

        if (readingCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }

        for (int i = 0; i < readingCount; i++) {
            System.out.println("\n" + (i + 1) + ". "
                    + readingList[i].title);
            System.out.println("Author: " + readingList[i].author);
            System.out.println("URL: " + readingList[i].url);
        }
    }

    static void removeBook() {

        if (readingCount == 0) {
            System.out.println("Your reading list is empty.");
            return;
        }

        viewReadingList();

        System.out.print("\nEnter book number to remove: ");

        try {
            int number = Integer.parseInt(input.nextLine());

            if (number < 1 || number > readingCount) {
                System.out.println("Invalid book number.");
                return;
            }

            for (int i = number - 1; i < readingCount - 1; i++) {
                readingList[i] = readingList[i + 1];
            }

            readingCount--;
            readingList[readingCount] = null;

            System.out.println("Book removed successfully!");

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    static void displayBookDetails() {

        if (bookCount == 0) {
            System.out.println("Please explore or search for books first.");
            return;
        }

        displayBooks();

        System.out.print("\nEnter book number: ");

        try {
            int number = Integer.parseInt(input.nextLine());

            if (number < 1 || number > bookCount) {
                System.out.println("Invalid book number.");
                return;
            }

            Book selectedBook = books[number - 1];

            System.out.println("\n===== BOOK DETAILS =====");
            System.out.println("Title: " + selectedBook.title);
            System.out.println("Author: " + selectedBook.author);
            System.out.println("Gutenberg URL: " + selectedBook.url);

        } catch (NumberFormatException e) {
            System.out.println("Please enter a valid number.");
        }
    }

    static class Book {

        String title;
        String author;
        String url;

        Book(String title, String author, String url) {
            this.title = title;
            this.author = author;
            this.url = url;
        }
    }
}
