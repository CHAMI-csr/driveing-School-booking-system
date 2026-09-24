-- --------------------------------------------------------
-- Host:                         127.0.0.1
-- Server version:               8.0.46 - MySQL Community Server - GPL
-- Server OS:                    Win64
-- HeidiSQL Version:             12.21.0.7344
-- --------------------------------------------------------

/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET NAMES utf8 */;
/*!50503 SET NAMES utf8mb4 */;
/*!40103 SET @OLD_TIME_ZONE=@@TIME_ZONE */;
/*!40103 SET TIME_ZONE='+00:00' */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;
/*!40111 SET @OLD_SQL_NOTES=@@SQL_NOTES, SQL_NOTES=0 */;


-- Dumping database structure for driving_school_db
CREATE DATABASE IF NOT EXISTS `driving_school_db` /*!40100 DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci */ /*!80016 DEFAULT ENCRYPTION='N' */;
USE `driving_school_db`;

-- Dumping structure for table driving_school_db.bookings
CREATE TABLE IF NOT EXISTS `bookings` (
  `booking_id` int NOT NULL AUTO_INCREMENT,
  `student_id` int NOT NULL,
  `instructor_id` int NOT NULL,
  `vehicle_id` int NOT NULL,
  `booking_date` date NOT NULL,
  `start_time` time NOT NULL,
  `end_time` time NOT NULL,
  `status` varchar(20) DEFAULT 'Booked',
  PRIMARY KEY (`booking_id`),
  KEY `student_id` (`student_id`),
  KEY `instructor_id` (`instructor_id`),
  KEY `vehicle_id` (`vehicle_id`),
  CONSTRAINT `bookings_ibfk_2` FOREIGN KEY (`instructor_id`) REFERENCES `instructors` (`instructor_id`),
  CONSTRAINT `bookings_ibfk_3` FOREIGN KEY (`vehicle_id`) REFERENCES `vehicles` (`vehicle_id`),
  CONSTRAINT `FK_bookings_students` FOREIGN KEY (`student_id`) REFERENCES `students` (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Dumping data for table driving_school_db.bookings: ~0 rows (approximately)
INSERT INTO `bookings` (`booking_id`, `student_id`, `instructor_id`, `vehicle_id`, `booking_date`, `start_time`, `end_time`, `status`) VALUES
	(1, 1, 1, 1, '2026-09-24', '11:00:00', '12:00:00', 'Confirmed');

-- Dumping structure for table driving_school_db.instructors
CREATE TABLE IF NOT EXISTS `instructors` (
  `instructor_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) NOT NULL,
  `phone` varchar(15) NOT NULL,
  `nic` varchar(12) DEFAULT NULL,
  `license_no` varchar(50) DEFAULT NULL,
  `vehicle_class` varchar(50) DEFAULT 'Class B (Dual Purpose / Car)',
  `status` varchar(20) DEFAULT 'Available',
  PRIMARY KEY (`instructor_id`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Dumping data for table driving_school_db.instructors: ~0 rows (approximately)
INSERT INTO `instructors` (`instructor_id`, `full_name`, `phone`, `nic`, `license_no`, `vehicle_class`, `status`) VALUES
	(1, 'ranasinhal', '0761042162', '200625104468', 'V31231', 'Class A & B (Combo)', 'Available');

-- Dumping structure for table driving_school_db.settings
CREATE TABLE IF NOT EXISTS `settings` (
  `default_password` varchar(255) DEFAULT NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Dumping data for table driving_school_db.settings: ~0 rows (approximately)

-- Dumping structure for table driving_school_db.students
CREATE TABLE IF NOT EXISTS `students` (
  `student_id` int NOT NULL AUTO_INCREMENT,
  `full_name` varchar(100) NOT NULL,
  `nic` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `phone` varchar(15) NOT NULL,
  `address` varchar(200) DEFAULT NULL,
  `vehicle_class` varchar(50) DEFAULT 'Class B (Dual Purpose / Car)',
  `status` varchar(30) DEFAULT 'Active Learner',
  PRIMARY KEY (`student_id`)
) ENGINE=InnoDB AUTO_INCREMENT=4 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Dumping data for table driving_school_db.students: ~0 rows (approximately)
INSERT INTO `students` (`student_id`, `full_name`, `nic`, `phone`, `address`, `vehicle_class`, `status`) VALUES
	(1, 'chamika', '200625103468', '0761042162', 'baddgama', 'Class A & B (Combo)', 'Active Learner'),
	(2, 'chamika', '200625103465', '0761042162', 'galle', 'Class B (Dual Purpose / Car)', 'Active Learner'),
	(3, 'afawww', '200625103469', '0716139367', 'faaf', 'Class B (Dual Purpose / Car)', 'Active Learner');

-- Dumping structure for table driving_school_db.users
CREATE TABLE IF NOT EXISTS `users` (
  `user_id` int NOT NULL AUTO_INCREMENT,
  `username` varchar(50) NOT NULL,
  `nic` varchar(12) CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci DEFAULT NULL,
  `password` varchar(255) NOT NULL,
  `role` varchar(50) DEFAULT NULL,
  `first_time` tinyint DEFAULT NULL,
  PRIMARY KEY (`user_id`),
  UNIQUE KEY `username` (`username`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Dumping data for table driving_school_db.users: ~2 rows (approximately)
INSERT INTO `users` (`user_id`, `username`, `nic`, `password`, `role`, `first_time`) VALUES
	(1, 'chamika', '200625103469', 'y9jNN1bntRvSHaVC886zoR146fe+lju83CLK2zk6uhw=', 'Admin', 0),
	(2, 'chamika1', '200625103468', '73l8gRjwLftklgfdXT+MdiMEjJwGPVMsyVxe16iYpk8=', 'Staff', 0);

-- Dumping structure for table driving_school_db.vehicles
CREATE TABLE IF NOT EXISTS `vehicles` (
  `vehicle_id` int NOT NULL AUTO_INCREMENT,
  `vehicle_number` varchar(20) NOT NULL,
  `vehicle_type` varchar(50) DEFAULT 'Car',
  `model` varchar(100) DEFAULT NULL,
  `vehicle_class` varchar(50) DEFAULT 'Class B (Dual Purpose / Car)',
  `transmission` varchar(20) DEFAULT 'Manual',
  `fuel_type` varchar(20) DEFAULT 'Petrol',
  `status` varchar(20) DEFAULT 'Available',
  `mileage` varchar(50) DEFAULT NULL,
  PRIMARY KEY (`vehicle_id`),
  UNIQUE KEY `vehicle_number` (`vehicle_number`)
) ENGINE=InnoDB AUTO_INCREMENT=2 DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Dumping data for table driving_school_db.vehicles: ~0 rows (approximately)
INSERT INTO `vehicles` (`vehicle_id`, `vehicle_number`, `vehicle_type`, `model`, `vehicle_class`, `transmission`, `fuel_type`, `status`, `mileage`) VALUES
	(1, 'BDA-6849', 'Class A (Motorcycle)', 'BDAQW', 'Class A (Motorcycle)', 'Auto', 'Petrol', 'Available', '500000');

/*!40103 SET TIME_ZONE=IFNULL(@OLD_TIME_ZONE, 'system') */;
/*!40101 SET SQL_MODE=IFNULL(@OLD_SQL_MODE, '') */;
/*!40014 SET FOREIGN_KEY_CHECKS=IFNULL(@OLD_FOREIGN_KEY_CHECKS, 1) */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40111 SET SQL_NOTES=IFNULL(@OLD_SQL_NOTES, 1) */;
