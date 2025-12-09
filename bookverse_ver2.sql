-- MySQL dump 10.13  Distrib 8.0.43, for Win64 (x86_64)
--
-- Host: 127.0.0.1    Database: bookverse_db
-- ------------------------------------------------------
-- Server version 9.5.0

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!50503 SET NAMES utf8 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;

-- TẠO DATABASE NẾU CHƯA CÓ
CREATE DATABASE IF NOT EXISTS bookverse_db
  DEFAULT CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;

USE bookverse_db;

--
-- Table structure for table `blog_details`
--

DROP TABLE IF EXISTS `blog_details`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blog_details` (
                                `detail_id` bigint NOT NULL AUTO_INCREMENT,
                                `content` varchar(255) DEFAULT NULL,
                                `created_at` datetime(6) DEFAULT NULL,
                                `blog_id` bigint DEFAULT NULL,
                                PRIMARY KEY (`detail_id`),
                                KEY `FK6jchr32lulbu2iv7wcfg3vgyu` (`blog_id`),
                                CONSTRAINT `FK6jchr32lulbu2iv7wcfg3vgyu` FOREIGN KEY (`blog_id`) REFERENCES `blogs` (`blog_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blog_details`
--

LOCK TABLES `blog_details` WRITE;
/*!40000 ALTER TABLE `blog_details` DISABLE KEYS */;
INSERT INTO `blog_details` VALUES (1,'Nothing','2025-11-15 15:59:22.158229',1),(2,'Nothing','2025-11-17 09:28:05.663358',3);
/*!40000 ALTER TABLE `blog_details` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `blogs`
--

DROP TABLE IF EXISTS `blogs`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `blogs` (
                         `blog_id` bigint NOT NULL AUTO_INCREMENT,
                         `author` varchar(255) DEFAULT NULL,
                         `created_at` datetime(6) DEFAULT NULL,
                         `thumbnail` varchar(255) DEFAULT NULL,
                         `title` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`blog_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `blogs`
--

LOCK TABLES `blogs` WRITE;
/*!40000 ALTER TABLE `blogs` DISABLE KEYS */;
INSERT INTO `blogs` VALUES (1,'Phanh','2025-11-15 15:59:22.041657','','how to love you'),(2,'Chu Lai','2025-11-17 09:27:13.113808','https://upload.wikimedia.org/wikipedia/vi/thumb/4/49/Mua_do_poster.jpg/250px-Mua_do_poster.jpg','Mua Do'),(3,'Chu Lai','2025-11-17 09:28:05.659071','https://upload.wikimedia.org/wikipedia/vi/thumb/4/49/Mua_do_poster.jpg/250px-Mua_do_poster.jpg','Mua Do');
/*!40000 ALTER TABLE `blogs` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `books`
--

DROP TABLE IF EXISTS `books`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `books` (
                         `book_id` bigint NOT NULL AUTO_INCREMENT,
                         `author` varchar(255) DEFAULT NULL,
                         `created_at` datetime(6) DEFAULT NULL,
                         `description` varchar(255) DEFAULT NULL,
                         `image_url` varchar(255) DEFAULT NULL,
                         `price` double DEFAULT NULL,
                         `stock` int NOT NULL,
                         `title` varchar(255) DEFAULT NULL,
                         `category_id` bigint DEFAULT NULL,
                         `discount_end` datetime(6) DEFAULT NULL,
                         `discount_percent` double DEFAULT NULL,
                         `discount_start` datetime(6) DEFAULT NULL,
                         `deleted` bit(1) DEFAULT NULL,
                         `active` bit(1) DEFAULT NULL,
                         PRIMARY KEY (`book_id`),
                         KEY `FKleqa3hhc0uhfvurq6mil47xk0` (`category_id`),
                         CONSTRAINT `FKleqa3hhc0uhfvurq6mil47xk0` FOREIGN KEY (`category_id`) REFERENCES `categories` (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=33 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `books`
--

LOCK TABLES `books` WRITE;
/*!40000 ALTER TABLE `books` DISABLE KEYS */;
INSERT INTO `books` VALUES
                        (1,'Alex Michaelides','2025-11-06 12:10:46.000000','A thrilling psychological mystery about a woman’s act of violence against her husband—and of the therapist obsessed with uncovering her motive.','/user/img/product/the_silent_patient.jpg',12.99,18,'The Silent Patient',1,'2025-11-20 23:59:00.000000',20,'2025-11-06 00:00:00.000000',_binary '\0',NULL),
                        (2,'Stephen Hawking','2025-11-06 12:10:46.000000','Hawking explores the origin and fate of the universe and unifies the laws of physics into one theory.','/user/img/product/the_theory_of_everything.jpg',15.5,35,'The Theory of Everything',2,NULL,NULL,NULL,NULL,NULL),
                        (3,'James Clear','2025-11-06 12:10:46.000000','An easy and proven way to build good habits and break bad ones.','/user/img/product/atomic_habits.jpg',14.75,37,'Atomic Habits',3,'2025-11-20 23:59:59.000000',25,'2025-11-06 00:00:00.000000',_binary '\0',NULL),
                        (4,'Robert T. Kiyosaki','2025-11-06 12:10:46.000000','The classic financial book that teaches how to make your money work for you.','/user/img/product/rich_dad_poor_dad.jpg',10.9,38,'Rich Dad Poor Dad',4,NULL,NULL,NULL,NULL,NULL),
                        (5,'J.K. Rowling','2025-11-06 12:10:46.000000','The first book in the magical Harry Potter series, introducing Hogwarts and the wizarding world.','/user/img/product/harry_potter_and_the_philosophers_stone.jpg',9.99,60,'Harry Potter and the Philosopher’s Stone',5,'2025-11-20 23:59:59.000000',30,'2025-11-06 00:00:00.000000',NULL,NULL),
                        (6,'Paul Deitel','2025-11-06 12:10:46.000000','Comprehensive guide to mastering Java programming with Spring and modern frameworks.','/user/img/product/advanced_java_programming.jpg',22,12,'Advanced Java Programming',6,'2025-11-20 23:59:59.000000',15,'2025-11-06 00:00:00.000000',_binary '\0',NULL),
                        (7,'Martin Gilbert','2025-11-06 12:10:46.000000','An in-depth and detailed account of World War II, from its origins to its aftermath.','/user/img/product/world_war_ii_a_complete_history.jpg',18.45,20,'World War II: A Complete History',7,NULL,NULL,NULL,NULL,NULL),
                        (8,'Eiichiro Oda','2025-11-06 12:10:46.000000','Luffy and his crew continue their thrilling adventure across the seas in the latest manga volume.','/user/img/product/one_piece_vol_101.jpg',7.5,80,'One Piece Vol. 101',8,NULL,NULL,NULL,NULL,NULL),
                        (9,'Phaidon Press','2025-11-06 12:10:46.000000','A beautiful collection of artworks and paintings from masters across history.','/user/img/product/the_art_book.jpg',25,10,'The Art Book',9,NULL,NULL,NULL,_binary '',NULL),
                        (10,'Lonely Planet','2025-11-06 12:10:46.000000','The ultimate travel guide to Japan with detailed maps and cultural tips.','/user/img/product/lonely_planet_japan.jpg',19.99,35,'Lonely Planet: Japan',10,NULL,NULL,NULL,NULL,NULL),
                        (11,'Tara Westover','2025-11-06 12:33:50.000000','A memoir about a woman who grows up in a strict and isolated household in rural Idaho and goes on to earn a PhD from Cambridge University.','/user/img/product/educated.jpg',13.9,35,'Educated',1,NULL,NULL,NULL,NULL,NULL),
                        (12,'Yuval Noah Harari','2025-11-06 12:33:50.000000','An exploration of the history of humankind from the Stone Age to the modern age.','/user/img/product/sapiens_a_brief_history_of_humankind.jpg',16.5,50,'Sapiens: A Brief History of Humankind',7,'2025-11-20 23:59:59.000000',10,'2025-11-06 00:00:00.000000',NULL,NULL),
                        (13,'Mark Manson','2025-11-06 12:33:50.000000','A counterintuitive approach to living a good life by focusing on what really matters.','/user/img/product/the_subtle_art_of_not_giving_a_fck.jpg',14.25,40,'The Subtle Art of Not Giving a F*ck',3,NULL,NULL,NULL,NULL,NULL),
                        (14,'Daniel Kahneman','2025-11-06 12:33:50.000000','A groundbreaking tour of how the mind works and how we can make better decisions.','/user/img/product/thinking_fast_and_slow.jpg',17.99,25,'Thinking, Fast and Slow',4,NULL,NULL,NULL,NULL,NULL),
                        (15,'Stephen R. Covey','2025-11-06 12:33:50.000000','Classic guide to personal and professional effectiveness.','/user/img/product/the_7_habits_of_highly_effective_people.jpg',15.25,40,'The 7 Habits of Highly Effective People',3,NULL,NULL,NULL,NULL,NULL),
                        (16,'Cal Newport','2025-11-06 12:33:50.000000','Rules for focused success in a distracted world.','/user/img/product/deep_work.jpg',13.5,40,'Deep Work',3,NULL,NULL,NULL,NULL,NULL),
                        (17,'Robert C. Martin','2025-11-06 12:33:50.000000','A handbook of agile software craftsmanship for writing clean, efficient, and maintainable code.','/user/img/product/clean_code.jpg',28.99,20,'Clean Code',6,'2025-11-20 23:59:59.000000',35,'2025-11-06 00:00:00.000000',NULL,NULL),
                        (18,'Erich Gamma et al.','2025-11-06 12:33:50.000000','A foundational text in software engineering, introducing reusable design solutions.','/user/img/product/design_patterns_elements_of_reusable_object_oriented_software.jpg',32.5,14,'Design Patterns: Elements of Reusable Object-Oriented Software',6,NULL,NULL,NULL,NULL,NULL),
                        (19,'Andrew Hunt & David Thomas','2025-11-06 12:33:50.000000','Essential tips and philosophy for professional software developers.','/user/img/product/the_pragmatic_programmer.jpg',26.75,17,'The Pragmatic Programmer',6,NULL,NULL,NULL,NULL,NULL),
                        (20,'Walter Isaacson','2025-11-06 12:33:50.000000','The authorized biography of Steve Jobs, based on hundreds of interviews.','/user/img/product/steve_jobs.jpg',19,48,'Steve Jobs',4,NULL,NULL,NULL,NULL,NULL),
                        (21,'Paulo Coelho','2025-11-06 12:33:50.000000','An inspiring story about a shepherd’s journey to find his personal legend.','/user/img/product/the_alchemist.jpg',11.99,60,'The Alchemist',1,NULL,NULL,NULL,NULL,NULL),
                        (22,'Michelle Obama','2025-11-06 12:33:50.000000','A deeply personal memoir by the former First Lady of the United States.','/user/img/product/becoming.jpg',17.49,34,'Becoming',1,NULL,NULL,NULL,NULL,NULL),
                        (23,'Héctor García & Francesc Miralles','2025-11-06 12:33:50.000000','Discover the Japanese philosophy of finding purpose and happiness in everyday life.','/user/img/product/ikigai_the_japanese_secret_to_a_long_and_happy_life.jpg',13.75,40,'Ikigai: The Japanese Secret to a Long and Happy Life',3,NULL,NULL,NULL,NULL,NULL),
                        (24,'Frank Herbert','2025-11-06 12:33:50.000000','Epic science fiction novel set in a distant future amidst a sprawling interstellar empire.','/user/img/product/dune.jpg',18.9,1,'Dune',1,NULL,NULL,NULL,NULL,NULL),
                        (25,'Jim Collins','2025-11-06 12:33:50.000000','Why some companies make the leap to success and others don’t.','/user/img/product/good_to_great.jpg',16.8,30,'Good to Great',4,NULL,NULL,NULL,NULL,NULL),
                        (26,'Stephen Hawking','2025-11-06 12:33:50.000000','Explains complex concepts of cosmology for the general reader.','/user/img/product/a_brief_history_of_time.jpg',14.99,26,'A Brief History of Time',2,NULL,0,NULL,_binary '\0',NULL),
                        (27,'Eric Ries','2025-11-06 12:33:50.000000','A guide to building a startup efficiently using lean principles.','/user/img/product/the_lean_startup.jpg',15.5,10,'The Lean Startup',4,NULL,NULL,NULL,NULL,NULL),
                        (28,'Antoine de Saint-Exupéry','2025-11-06 12:33:50.000000','A poetic tale of loneliness, friendship, and love.','/user/img/product/the_little_prince.jpg',9.5,70,'The Little Prince',5,'2025-11-20 23:59:59.000000',40,'2025-11-06 00:00:00.000000',NULL,NULL),
                        (29,'Hajime Isayama','2025-11-06 12:33:50.000000','Humanity fights for survival against giant humanoid Titans.','/user/img/product/manga_attack_on_titan_vol_1.jpg',8.99,89,'Manga: Attack on Titan Vol. 1',8,NULL,NULL,NULL,_binary '\0',NULL),
                        (30,'Lonely Planet Team','2025-11-06 12:33:50.000000','Essential travel hacks and affordable destinations for backpackers.','/user/img/product/travel_the_world_on_a_budget.jpg',12.49,25,'Travel the World on a Budget',10,NULL,NULL,NULL,NULL,NULL),
                        (31,'bokkkkkkkk','2025-11-11 09:16:53.745022','abbcccccbcbccbc','/user/img/product/986368ec-1966-4793-b5ee-37796ea4bb2a.png',20,266,'Boookkkkkk',3,'2025-11-10 09:16:00.000000',0.2,'2025-11-14 09:15:00.000000',_binary '',NULL),
                        (32,'Phanh','2025-11-26 15:30:08.464464','abcdef\r\n','/user/img/product/874a0004-9e8a-4473-b814-fd633723974b.png',1000,0,'abcdef',2,'2025-11-29 15:29:00.000000',10,'2025-11-27 15:29:00.000000',_binary '\0',NULL);
/*!40000 ALTER TABLE `books` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `categories`
--

DROP TABLE IF EXISTS `categories`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `categories` (
                              `category_id` bigint NOT NULL AUTO_INCREMENT,
                              `description` varchar(255) DEFAULT NULL,
                              `name` varchar(255) DEFAULT NULL,
                              PRIMARY KEY (`category_id`)
) ENGINE=InnoDB AUTO_INCREMENT=11 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `categories`
--

LOCK TABLES `categories` WRITE;
/*!40000 ALTER TABLE `categories` DISABLE KEYS */;
INSERT INTO `categories` VALUES
                             (1,'Novels and stories that describe imaginary events and people.','Fiction'),
                             (2,'Books about scientific discoveries and modern technology.','Science & Technology'),
                             (3,'Guides for personal growth, productivity, and mindset.','Self-Help'),
                             (4,'Books about management, startups, and finance.','Business & Economics'),
                             (5,'Colorful and fun books for children.','Children'),
                             (6,'Academic books and study materials.','Education'),
                             (7,'Books covering global and local historical events.','History'),
                             (8,'Illustrated stories and Japanese manga.','Comics & Manga'),
                             (9,'Books about visual arts and creative photography.','Art & Photography'),
                             (10,'Travel guides and adventure stories.','Travel');
/*!40000 ALTER TABLE `categories` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `coupons`
--

DROP TABLE IF EXISTS `coupons`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `coupons` (
                           `coupon_id` bigint NOT NULL AUTO_INCREMENT,
                           `active` bit(1) NOT NULL,
                           `code` varchar(50) NOT NULL,
                           `created_at` datetime(6) DEFAULT NULL,
                           `description` varchar(255) DEFAULT NULL,
                           `discount_type` varchar(20) NOT NULL,
                           `discount_value` double NOT NULL,
                           `expiry_date` datetime(6) NOT NULL,
                           `max_discount_amount` double DEFAULT NULL,
                           `min_purchase_amount` double NOT NULL,
                           `usage_limit` int DEFAULT NULL,
                           `used_count` int NOT NULL,
                           PRIMARY KEY (`coupon_id`),
                           UNIQUE KEY `UKeplt0kkm9yf2of2lnx6c1oy9b` (`code`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `coupons`
--

LOCK TABLES `coupons` WRITE;
/*!40000 ALTER TABLE `coupons` DISABLE KEYS */;
INSERT INTO `coupons` VALUES
                          (1,_binary '','WELCOME10','2025-11-14 09:49:14.000000','Welcome coupon: 10% off up to $20','PERCENTAGE',10,'2025-12-31 23:59:59.000000',20,0,100,0),
                          (2,_binary '','SAVE20','2025-11-14 09:49:14.000000','Save 20% on orders over $50, maximum discount $50','PERCENTAGE',20,'2025-12-31 23:59:59.000000',50,50,50,0),
                          (3,_binary '','FIXED5','2025-11-14 09:49:14.000000','Fixed $5 off any order','FIXED_AMOUNT',5,'2025-12-31 23:59:59.000000',NULL,0,NULL,0),
                          (4,_binary '','BIG50','2025-11-14 09:49:14.000000','$50 off orders over $100 (Limited to 10 uses)','FIXED_AMOUNT',50,'2025-12-31 23:59:59.000000',NULL,100,10,0);
/*!40000 ALTER TABLE `coupons` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `inventory_transactions`
--

DROP TABLE IF EXISTS `inventory_transactions`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `inventory_transactions` (
                                          `transaction_id` bigint NOT NULL AUTO_INCREMENT,
                                          `created_at` datetime(6) NOT NULL,
                                          `note` text,
                                          `quantity` int NOT NULL,
                                          `reason` varchar(255) DEFAULT NULL,
                                          `reference_id` bigint DEFAULT NULL,
                                          `reference_type` varchar(50) DEFAULT NULL,
                                          `stock_after` int NOT NULL,
                                          `stock_before` int NOT NULL,
                                          `transaction_type` varchar(20) NOT NULL,
                                          `book_id` bigint NOT NULL,
                                          `created_by` bigint DEFAULT NULL,
                                          PRIMARY KEY (`transaction_id`),
                                          KEY `FKauuv7tp43f09wgbm4kuxpn1e2` (`book_id`),
                                          KEY `FKjcpd725xjfv6sovo5t7y33b3f` (`created_by`),
                                          CONSTRAINT `FKauuv7tp43f09wgbm4kuxpn1e2` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`),
                                          CONSTRAINT `FKjcpd725xjfv6sovo5t7y33b3f` FOREIGN KEY (`created_by`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=16 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `inventory_transactions`
--

LOCK TABLES `inventory_transactions` WRITE;
/*!40000 ALTER TABLE `inventory_transactions` DISABLE KEYS */;
INSERT INTO `inventory_transactions` VALUES
                                         (1,'2025-11-20 09:37:27.398253','',10,'Restock',NULL,'MANUAL',35,25,'IMPORT',2,1),
                                         (2,'2025-11-20 09:37:51.548302','',15,'Return from Supplier',NULL,'MANUAL',45,30,'IMPORT',16,1),
                                         (3,'2025-11-20 09:38:15.174092','',-12,'Lost',NULL,'MANUAL',38,50,'EXPORT',4,1),
                                         (4,'2025-11-20 09:38:32.207754','',-5,'Damaged',NULL,'MANUAL',40,45,'EXPORT',16,1),
                                         (5,'2025-11-20 09:38:53.894734','',-5,'Stock Count Correction',NULL,'MANUAL',40,45,'ADJUSTMENT',15,1),
                                         (6,'2025-11-20 09:39:15.729647','',20,'New Purchase',NULL,'MANUAL',48,28,'IMPORT',20,1),
                                         (7,'2025-11-20 09:42:30.786401','Stock deducted for order #27',-1,'Order processed',27,'ORDER',16,17,'EXPORT',19,2),
                                         (8,'2025-11-20 09:42:30.793006','Stock deducted for order #27',-1,'Order processed',27,'ORDER',12,13,'EXPORT',6,2),
                                         (9,'2025-11-20 09:43:29.114552','',-23,'Return to Supplier',NULL,'MANUAL',1,24,'EXPORT',24,1),
                                         (10,'2025-11-20 09:43:55.627724','',-20,'Return to Supplier',NULL,'MANUAL',0,20,'EXPORT',27,1),
                                         (11,'2025-11-20 10:32:39.149534','',-10,'Return to Supplier',NULL,'MANUAL',18,28,'EXPORT',1,1),
                                         (12,'2025-11-20 10:34:01.188111','Stock deducted for order #28',-1,'Order processed',28,'ORDER',13,14,'EXPORT',18,2),
                                         (13,'2025-11-20 10:34:01.190309','Stock deducted for order #28',-1,'Order processed',28,'ORDER',11,12,'EXPORT',6,2),
                                         (14,'2025-11-20 10:35:21.676265','',10,'Restock',NULL,'MANUAL',10,0,'IMPORT',27,1),
                                         (15,'2025-12-09 10:57:49.309868','Stock deducted for order #29',-2,'Order processed',29,'ORDER',24,26,'EXPORT',26,2);
/*!40000 ALTER TABLE `inventory_transactions` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `order_items`
--

DROP TABLE IF EXISTS `order_items`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `order_items` (
                               `item_id` bigint NOT NULL AUTO_INCREMENT,
                               `price` double DEFAULT NULL,
                               `quantity` int NOT NULL,
                               `book_id` bigint DEFAULT NULL,
                               `order_id` bigint DEFAULT NULL,
                               PRIMARY KEY (`item_id`),
                               KEY `FKi4ptndslo2pyfp9r1x0eulh9g` (`book_id`),
                               KEY `FKbioxgbv59vetrxe0ejfubep1w` (`order_id`),
                               CONSTRAINT `FKbioxgbv59vetrxe0ejfubep1w` FOREIGN KEY (`order_id`) REFERENCES `orders` (`order_id`),
                               CONSTRAINT `FKi4ptndslo2pyfp9r1x0eulh9g` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`)
) ENGINE=InnoDB AUTO_INCREMENT=35 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `order_items`
--

LOCK TABLES `order_items` WRITE;
/*!40000 ALTER TABLE `order_items` DISABLE KEYS */;
INSERT INTO `order_items` VALUES
                              (13,18.7,1,6,11),
                              (15,18.7,1,6,13),
                              (16,11.06,1,3,14),
                              (17,18.7,1,6,15),
                              (18,11.06,1,3,16),
                              (19,17.49,1,22,17),
                              (20,18.84,1,17,18),
                              (21,17.49,1,22,19),
                              (22,18.7,1,6,20),
                              (23,18.7,1,6,21),
                              (24,8.99,1,29,22),
                              (25,18.84,1,17,23),
                              (26,17.99,1,14,24),
                              (27,14.99,1,26,25),
                              (28,18.7,1,6,26),
                              (29,14.99,1,26,26),
                              (30,26.75,1,19,27),
                              (31,18.7,1,6,27),
                              (32,32.5,1,18,28),
                              (33,18.7,1,6,28),
                              (34,14.99,2,26,29);
/*!40000 ALTER TABLE `order_items` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `orders`
--

DROP TABLE IF EXISTS `orders`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `orders` (
                          `order_id` bigint NOT NULL AUTO_INCREMENT,
                          `address` varchar(255) DEFAULT NULL,
                          `created_at` datetime(6) DEFAULT NULL,
                          `note` varchar(255) DEFAULT NULL,
                          `status` varchar(255) DEFAULT NULL,
                          `total_amount` double DEFAULT NULL,
                          `user_id` bigint DEFAULT NULL,
                          `vnp_bank_code` varchar(255) DEFAULT NULL,
                          `vnp_pay_date` varchar(255) DEFAULT NULL,
                          `vnp_response_code` varchar(255) DEFAULT NULL,
                          `vnp_transaction_no` varchar(255) DEFAULT NULL,
                          PRIMARY KEY (`order_id`),
                          KEY `FK32ql8ubntj5uh44ph9659tiih` (`user_id`),
                          CONSTRAINT `FK32ql8ubntj5uh44ph9659tiih` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=30 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `orders`
--

LOCK TABLES `orders` WRITE;
/*!40000 ALTER TABLE `orders` DISABLE KEYS */;
INSERT INTO `orders` VALUES
                         (11,'Nghi Xuân','2025-11-13 16:42:12.951320','aaaa','pending',18.7,2,NULL,NULL,NULL,NULL),
                         (13,'Nghi Xuân','2025-11-13 23:05:23.723305','','pending',18.7,2,NULL,NULL,NULL,NULL),
                         (14,'Nghi Xuân','2025-11-13 23:19:15.679785','','pending',11.06,2,NULL,NULL,NULL,NULL),
                         (15,'Nghi Xuân','2025-11-13 23:19:49.369404','','pending',18.7,2,NULL,NULL,NULL,NULL),
                         (16,'Nghi Xuân,Việt Nam','2025-11-13 23:32:29.175155','','processing',11.06,2,NULL,NULL,NULL,NULL),
                         (17,'Nghi Xuân','2025-11-13 23:35:25.065131','','processing',17.49,2,NULL,NULL,NULL,NULL),
                         (18,'Nghi Xuân','2025-11-13 23:58:54.318907','','pending',18.84,2,NULL,NULL,NULL,NULL),
                         (19,'Nghi Xuân','2025-11-14 07:52:34.525973','','pending',17.49,2,NULL,NULL,NULL,NULL),
                         (20,'Nghi Xuân','2025-11-14 08:02:32.621047','','pending',18.7,2,NULL,NULL,NULL,NULL),
                         (21,'Nghi Xuân','2025-11-14 09:15:34.771583','','pending',18.7,2,NULL,NULL,NULL,NULL),
                         (22,'Nghi Xuân','2025-11-15 10:36:45.536496','','processing',8.99,2,NULL,NULL,NULL,NULL),
                         (23,'Nghi Xuân','2025-11-15 11:41:21.955515','','pending',18.84,2,NULL,NULL,NULL,NULL),
                         (24,'Nghi Xuân','2025-11-15 11:50:38.023183','','pending',17.99,2,NULL,NULL,NULL,NULL),
                         (25,'Nghi Xuân','2025-11-15 11:53:08.336938','','pending',14.99,2,NULL,NULL,NULL,NULL),
                         (26,'Nghi Xuân','2025-11-17 09:18:26.560199','remind for the shipper','shipped',30.32,2,NULL,NULL,NULL,NULL),
                         (27,'Nghi Xuân','2025-11-20 09:41:22.628148','','processing',45.45,2,NULL,NULL,NULL,NULL),
                         (28,'Nghi Xuân','2025-11-20 10:33:28.673529','','processing',51.2,2,NULL,NULL,NULL,NULL),
                         (29,'Nghi Xuân','2025-12-09 10:54:17.055293','abc','shipped',26.98,2,NULL,NULL,NULL,NULL);
/*!40000 ALTER TABLE `orders` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `reviews`
--

DROP TABLE IF EXISTS `reviews`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `reviews` (
                           `review_id` bigint NOT NULL AUTO_INCREMENT,
                           `comment` varchar(255) DEFAULT NULL,
                           `created_at` datetime(6) DEFAULT NULL,
                           `rating` int NOT NULL,
                           `book_id` bigint DEFAULT NULL,
                           `user_id` bigint DEFAULT NULL,
                           `visible` bit(1) DEFAULT NULL,
                           PRIMARY KEY (`review_id`),
                           KEY `FK6a9k6xvev80se5rreqvuqr7f9` (`book_id`),
                           KEY `FKcgy7qjc1r99dp117y9en6lxye` (`user_id`),
                           CONSTRAINT `FK6a9k6xvev80se5rreqvuqr7f9` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`),
                           CONSTRAINT `FKcgy7qjc1r99dp117y9en6lxye` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `reviews`
--

LOCK TABLES `reviews` WRITE;
/*!40000 ALTER TABLE `reviews` DISABLE KEYS */;
INSERT INTO `reviews` VALUES
                          (1,'this book is good','2025-11-15 16:04:20.838065',4,24,2,_binary ''),
                          (2,'Nice\r\n','2025-11-17 09:14:18.749291',5,26,2,_binary ''),
                          (3,'Wonderful!!!','2025-12-09 10:49:49.393047',3,26,2,_binary '');
/*!40000 ALTER TABLE `reviews` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `roles`
--

DROP TABLE IF EXISTS `roles`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `roles` (
                         `role_id` bigint NOT NULL AUTO_INCREMENT,
                         `name` varchar(255) DEFAULT NULL,
                         PRIMARY KEY (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `roles`
--

LOCK TABLES `roles` WRITE;
/*!40000 ALTER TABLE `roles` DISABLE KEYS */;
INSERT INTO `roles` VALUES (1,'USER'),(2,'ADMIN');
/*!40000 ALTER TABLE `roles` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `users`
--

DROP TABLE IF EXISTS `users`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `users` (
                         `user_id` bigint NOT NULL AUTO_INCREMENT,
                         `address` varchar(255) DEFAULT NULL,
                         `created_at` datetime(6) DEFAULT NULL,
                         `email` varchar(255) DEFAULT NULL,
                         `full_name` varchar(255) DEFAULT NULL,
                         `password` varchar(255) DEFAULT NULL,
                         `phone` varchar(255) DEFAULT NULL,
                         `username` varchar(255) DEFAULT NULL,
                         `role_id` bigint DEFAULT NULL,
                         `deleted` bit(1) DEFAULT NULL,
                         PRIMARY KEY (`user_id`),
                         KEY `FKp56c1712k691lhsyewcssf40f` (`role_id`),
                         CONSTRAINT `FKp56c1712k691lhsyewcssf40f` FOREIGN KEY (`role_id`) REFERENCES `roles` (`role_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `users`
--

LOCK TABLES `users` WRITE;
/*!40000 ALTER TABLE `users` DISABLE KEYS */;
INSERT INTO `users` VALUES
                        (1,'Nghi Xuân','2025-11-10 09:07:17.150395','nguyenxuanphananh@gmail.com','Nguyễn Phan Anh','$2a$10$bA4ZAKG0aWcCIKfitn10FuE/vAxGHPX9eVqLdKp0BhTFg/l.Xhsvu','0777451107','dracoyoungboi',2,_binary '\0'),
                        (2,'Nghi Xuân','2025-11-10 09:08:33.495626','anhnphe186085@fpt.edu.vn','Nguyễn Phan Anh','$2a$10$VxBCValSwYbzgS.eVJK1nOJ/xfLPpyrgneOMvEgOmZEW9xwuz1MFm','0832132728','draco',1,_binary '\0'),
                        (3,'Nghi Xuân','2025-11-11 09:06:06.875585','nguyenxuanphananh@gmail.com','Phanh','$2a$10$7pyeFUCt9ypCKXO3uXUT7eg7dvvjQXtZPEpoNLN7XiiOGoOUo4nY6','07775611107','dracoo',1,_binary '\0'),
                        (4,'abccccccc','2025-11-11 09:14:32.150010','dangwkhiem.work@gmail.com','Phan Anh','$2a$10$lbOS7nWEK/lF70E8A.27H.pajhcOeG2qAIATOsheP0aP96j9E3c2','0832132728','Phanh',1,_binary '\0');
/*!40000 ALTER TABLE `users` ENABLE KEYS */;
UNLOCK TABLES;

--
-- Table structure for table `wishlist`
--

DROP TABLE IF EXISTS `wishlist`;
/*!40101 SET @saved_cs_client     = @@character_set_client */;
/*!50503 SET character_set_client = utf8mb4 */;
CREATE TABLE `wishlist` (
                            `wishlist_id` bigint NOT NULL AUTO_INCREMENT,
                            `added_at` datetime(6) DEFAULT NULL,
                            `book_id` bigint DEFAULT NULL,
                            `user_id` bigint DEFAULT NULL,
                            PRIMARY KEY (`wishlist_id`),
                            KEY `FKm5enjhac8nch6sen9m1th9gkw` (`book_id`),
                            KEY `FKtrd6335blsefl2gxpb8lr0gr7` (`user_id`),
                            CONSTRAINT `FKm5enjhac8nch6sen9m1th9gkw` FOREIGN KEY (`book_id`) REFERENCES `books` (`book_id`),
                            CONSTRAINT `FKtrd6335blsefl2gxpb8lr0gr7` FOREIGN KEY (`user_id`) REFERENCES `users` (`user_id`)
) ENGINE=InnoDB AUTO_INCREMENT=5 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;
/*!40101 SET character_set_client = @saved_cs_client */;

--
-- Dumping data for table `wishlist`
--

LOCK TABLES `wishlist` WRITE;
/*!40000 ALTER TABLE `wishlist` DISABLE KEYS */;
INSERT INTO `wishlist` VALUES (4,'2025-12-09 10:49:22.612519',26,2);
/*!40000 ALTER TABLE `wishlist` ENABLE KEYS */;
UNLOCK TABLES;

/*!40103 SET TIME_ZONE=@OLD_TIME_ZONE */;
/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40111 SET SQL_NOTES=@OLD_SQL_NOTES */;

-- Dump completed on 2025-12-09 13:33:15
