-- MySQL Administrator dump 1.4
--
-- ------------------------------------------------------
-- Server version	5.0.67-community-nt


/*!40101 SET @OLD_CHARACTER_SET_CLIENT=@@CHARACTER_SET_CLIENT */;
/*!40101 SET @OLD_CHARACTER_SET_RESULTS=@@CHARACTER_SET_RESULTS */;
/*!40101 SET @OLD_COLLATION_CONNECTION=@@COLLATION_CONNECTION */;
/*!40101 SET NAMES utf8 */;

/*!40014 SET @OLD_UNIQUE_CHECKS=@@UNIQUE_CHECKS, UNIQUE_CHECKS=0 */;
/*!40014 SET @OLD_FOREIGN_KEY_CHECKS=@@FOREIGN_KEY_CHECKS, FOREIGN_KEY_CHECKS=0 */;
/*!40101 SET @OLD_SQL_MODE=@@SQL_MODE, SQL_MODE='NO_AUTO_VALUE_ON_ZERO' */;


--
-- Create schema genopub
--

CREATE DATABASE IF NOT EXISTS genopub;
USE genopub;

--
-- Definition of table `AnalysisType`
--

DROP TABLE IF EXISTS `AnalysisType`;
CREATE TABLE `AnalysisType` (
  `idAnalysisType` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  `isActive` char(1) default NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY  USING BTREE (`idAnalysisType`),
  KEY `FK_AnalysisType_User` (`idUser`),
  CONSTRAINT `FK_AnalysisType_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `AnalysisType`
--

/*!40000 ALTER TABLE `AnalysisType` DISABLE KEYS */;
INSERT INTO `AnalysisType` (`idAnalysisType`,`name`,`isActive`,`idUser`) VALUES 
 (1,'Dynamic/Differential','Y',NULL),
 (2,'Static','Y',NULL);
/*!40000 ALTER TABLE `AnalysisType` ENABLE KEYS */;


--
-- Definition of table `Annotation`
--

DROP TABLE IF EXISTS `Annotation`;
CREATE TABLE `Annotation` (
  `idAnnotation` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(2000) NOT NULL,
  `description` varchar(10000) default NULL,
  `fileName` varchar(2000) default NULL,
  `idExperimentPlatform` int(10) unsigned default NULL,
  `idExperimentMethod` int(10) unsigned default NULL,
  `idAnalysisType` int(10) unsigned default NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  `codeVisibility` varchar(10) NOT NULL,
  `idUser` int(10) unsigned default NULL,
  `idUserGroup` int(10) unsigned default NULL,
  `summary` varchar(5000) default NULL,
  `createdBy` varchar(200) default NULL,
  `createDate` datetime default NULL,  
  `isLoaded` char(1) default 'N',  
  PRIMARY KEY  (`idAnnotation`),
  KEY `FK_Annotation_ExperimentPlatform` (`idExperimentPlatform`),
  KEY `FK_Annotation_ExperimentMethod` (`idExperimentMethod`),
  KEY `FK_Annotation_GenomeVersion` (`idGenomeVersion`),
  KEY `FK_Annotation_User` (`idUser`),
  KEY `FK_Annotation_Visibility` (`codeVisibility`),
  KEY `FK_Annotation_group` USING BTREE (`idUserGroup`),
  KEY `FK_Annotation_QuantificationType` USING BTREE (`idAnalysisType`),
  CONSTRAINT `FK_Annotation_AnalysisType` FOREIGN KEY (`idAnalysisType`) REFERENCES `AnalysisType` (`idAnalysisType`),
  CONSTRAINT `FK_Annotation_ExperimentMethod` FOREIGN KEY (`idExperimentMethod`) REFERENCES `ExperimentMethod` (`idExperimentMethod`),
  CONSTRAINT `FK_Annotation_ExperimentPlatform` FOREIGN KEY (`idExperimentPlatform`) REFERENCES `ExperimentPlatform` (`idExperimentPlatform`),
  CONSTRAINT `FK_Annotation_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_Annotation_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_Annotation_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_Annotation_Visibility` FOREIGN KEY (`codeVisibility`) REFERENCES `Visibility` (`codeVisibility`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Annotation`
--

/*!40000 ALTER TABLE `Annotation` DISABLE KEYS */;
/*!40000 ALTER TABLE `Annotation` ENABLE KEYS */;


--
-- Definition of table `AnnotationGrouping`
--

DROP TABLE IF EXISTS `AnnotationGrouping`;
CREATE TABLE `AnnotationGrouping` (
  `idAnnotationGrouping` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(2000) NOT NULL,
  `description` varchar(10000) default NULL,
  `idParentAnnotationGrouping` int(10) unsigned default NULL,
  `idGenomeVersion` int(10) unsigned  NOT NULL,
  `idUserGroup` int(10) unsigned default NULL,
  `createdBy` varchar(200) default NULL,
  `createDate` datetime default NULL,  
  PRIMARY KEY  USING BTREE (`idAnnotationGrouping`),
  KEY `FK_AnnotationGrouping_GenomeVersion` (`idGenomeVersion`),
  KEY `FK_AnnotationGrouping_parentAnnotationGrouping` USING BTREE (`idParentAnnotationGrouping`),
  KEY `FK_AnnotationGrouping_UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_AnnotationGrouping_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_AnnotationGrouping_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_AnnotationGrouping_parentAnnotationGrouping` FOREIGN KEY (`idParentAnnotationGrouping`) REFERENCES `AnnotationGrouping` (`idAnnotationGrouping`)
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `AnnotationGrouping`
--

/*!40000 ALTER TABLE `AnnotationGrouping` DISABLE KEYS */;
INSERT INTO `AnnotationGrouping` (`idAnnotationGrouping`,`name`,`description`,`idParentAnnotationGrouping`,`idGenomeVersion`,`idUserGroup`) VALUES 
 (400,'A_thaliana_Jan_2004','A_thaliana_Jan_2004',NULL,400,NULL),
 (500,'B_taurus_Aug_2006','B_taurus_Aug_2006',NULL,500,NULL),
 (501,'B_taurus_Oct_2007','B_taurus_Oct_2007',NULL,501,NULL),
 (1200,'C_elegans_May_2003','C_elegans_May_2003',NULL,1200,NULL),
 (1201,'C_elegans_Mar_2004','C_elegans_Mar_2004',NULL,1201,NULL),
 (1202,'C_elegans_Jan_2007','C_elegans_Jan_2007',NULL,1202,NULL),
 (1203,'C_elegans_May_2007','C_elegans_May_2007',NULL,1203,NULL),
 (1204,'C_elegans_May_2008','C_elegans_May_2008',NULL,1204,NULL),
 (1500,'C_familiaris_May_2005','C_familiaris_May_2005',NULL,1500,NULL),
 (1900,'D_melanogaster_Apr_2004','D_melanogaster_Apr_2004',NULL,1900,NULL),
 (1901,'D_melanogaster_Apr_2006','D_melanogaster_Apr_2006',NULL,1901,NULL),
 (1902,'D_melanogaster_Jan_2003','D_melanogaster_Jan_2003',NULL,1902,NULL),
 (2401,'H_sapiens_Nov_2002','H_sapiens_Nov_2002',NULL,2401,NULL),
 (2403,'H_sapiens_Jul_2003','H_sapiens_Jul_2003',NULL,2403,NULL),
 (2700,'D_rerio_Mar_2006','D_rerio_Mar_2006',NULL,2700,NULL),
 (2701,'D_rerio_Jul_2007','D_rerio_Jul_2007',NULL,2701,NULL),
 (2900,'E_coli_Oct_2007','E_coli_Oct_2007',NULL,2900,NULL),
 (3100,'G_gallus_May_2006','G_gallus_May_2006',NULL,3100,NULL),
 (3301,'G_max_Dec_2008','G_max_Dec_2008',NULL,3301,NULL),
 (3400, 'H_sapiens_Jun_2002', 'H_sapiens_Jun_2002',NULL,3400,NULL),
 (3401, 'H_sapiens_Nov_2002', 'H_sapiens_Nov_2002',NULL,3401,NULL),
 (3402,'H_sapiens_Apr_2003','H_sapiens_Apr_2003',NULL,3402,NULL),
 (3403, 'H_sapiens_Jul_2003', 'H_sapiens_Jul_2003',NULL,3403,NULL),
 (3404,'H_sapiens_May_2004','H_sapiens_May_2004',NULL,3404,NULL),
 (3405,'H_sapiens_Mar_2006','H_sapiens_Mar_2006',NULL,3405,NULL),
 (3500,'M_mulatta_Jan_2006','M_mulatta_Jan_2006',NULL,3500,NULL),
 (3600,'M_truncatula_Aug_2007','M_truncatula_Aug_2007',NULL,3600,NULL),
 (3800,'M_musculus_Feb_2002','M_musculus_Feb_2002',NULL,3800,NULL),
 (3801,'M_musculus_Feb_2003','M_musculus_Feb_2003',NULL,3801,NULL),
 (3802,'M_musculus_Oct_2003','M_musculus_Oct_2003',NULL,3802,NULL),
 (3803,'M_musculus_Aug_2005','M_musculus_Aug_2005',NULL,3803,NULL),
 (3804,'M_musculus_Mar_2006','M_musculus_Mar_2006',NULL,3804,NULL),
 (3806,'M_musculus_Jul_2007','M_musculus_Jul_2007',NULL,3806,NULL),
 (3900,'M_abscessus_Mar_2008','M_abscessus_Mar_2008',NULL,3900,NULL),
 (4000,'M_smegmatis_Mar_2008','M_smegmatis_Mar_2008',NULL,4000,NULL),
 (4100,'M_tuberculosis_Sep_2008','M_tuberculosis_Sep_2008',NULL,4100,NULL),
 (4300,'O_sativa_Jan_2007','O_sativa_Jan_2007',NULL,4300,NULL),
 (4301,'O_sativa_Jan_2009','O_sativa_Jan_2009',NULL,4301,NULL),
 (4302,'O_sativa_Jun_2009','O_sativa_Jun_2009',NULL,4302,NULL),
 (4800,'P_falciparum_Jul_2007','P_falciparum_Jul_2007',NULL,4800,NULL),
 (5000,'P_trichocarpa_Jun_2004','P_trichocarpa_Jun_2004',NULL,5000,NULL),
 (5200,'R_norvegicus_Jan_2003','R_norvegicus_Jan_2003',NULL,5200,NULL),
 (5201,'R_norvegicus_Jun_2003','R_norvegicus_Jun_2003',NULL,5201,NULL),
 (5202,'R_norvegicus_Nov_2004','R_norvegicus_Nov_2004',NULL,5202,NULL),
 (5207,'S_cerevisiae_May_2008','S_cerevisiae_May_2008',NULL,5207,NULL),
 (5300,'S_cerevisiae_Oct_2003','S_cerevisiae_Oct_2003',NULL,5300,NULL),
 (5304,'S_cerevisiae_Feb_2006','S_cerevisiae_Feb_2006',NULL,5304,NULL),
 (5305,'S_cerevisiae_Jul_2007','S_cerevisiae_Jul_2007',NULL,5305,NULL),
 (5306,'S_cerevisiae_Apr_2008','S_cerevisiae_Apr_2008',NULL,5306,NULL),
 (5400,'S_pombe_Sep_2004','S_pombe_Sep_2004',NULL,5400,NULL),
 (5401,'S_pombe_Apr_2007','S_pombe_Apr_2007',NULL,5401,NULL),
 (5402,'S_pombe_Sep_2007','S_pombe_Sep_2007',NULL,5402,NULL),
 (5500, 'S_glossinidius_Jan_2006', 'S_glossinidius_Jan_2006', NULL, 5500, NULL),
 (6000,'T_nigroviridis_Feb_2004','T_nigroviridis_Feb_2004',NULL,6000,NULL),
 (6100,'V_vinifera_Apr_2007','V_vinifera_Apr_2007',NULL,6100,NULL),
 (6201,'X_tropicalis_Aug_2005','X_tropicalis_Aug_2005',NULL,6201,NULL);
/*!40000 ALTER TABLE `AnnotationGrouping` ENABLE KEYS */;


--
-- Definition of table `AnnotationToAnnotationGrouping`
--

DROP TABLE IF EXISTS `AnnotationToAnnotationGrouping`;
CREATE TABLE `AnnotationToAnnotationGrouping` (
  `idAnnotation` int(10) unsigned NOT NULL,
  `idAnnotationGrouping` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idAnnotation`,`idAnnotationGrouping`),
  KEY `FK_AnnotationToAnnotationGrouping_AnnotationGrouping` (`idAnnotationGrouping`),
  CONSTRAINT `FK_AnnotationToAnnotationGrouping_AnnotationGrouping` FOREIGN KEY (`idAnnotationGrouping`) REFERENCES `AnnotationGrouping` (`idAnnotationGrouping`),
  CONSTRAINT `FK_AnnotationToGrouping_Annotation` FOREIGN KEY (`idAnnotation`) REFERENCES `Annotation` (`idAnnotation`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `AnnotationToAnnotationGrouping`
--

/*!40000 ALTER TABLE `AnnotationToAnnotationGrouping` DISABLE KEYS */;
/*!40000 ALTER TABLE `AnnotationToAnnotationGrouping` ENABLE KEYS */;


--
-- Definition of table `ExperimentMethod`
--

DROP TABLE IF EXISTS `ExperimentMethod`;
CREATE TABLE `ExperimentMethod` (
  `idExperimentMethod` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  `isActive` char(1) default NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY  (`idExperimentMethod`),
  KEY `FK_ExperimentMethod_User` (`idUser`),
  CONSTRAINT `FK_ExperimentMethod_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=9 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `ExperimentMethod`
--

/*!40000 ALTER TABLE `ExperimentMethod` DISABLE KEYS */;
INSERT INTO `ExperimentMethod` (`idExperimentMethod`,`name`,`isActive`,`idUser`) VALUES 
 (1,'chIP-seq','Y',NULL),
 (2,'chIP-chip','Y',NULL),
 (3,'CGN Microarray','Y',NULL),
 (4,'SNP Microarray','Y',NULL),
 (5,'Transcriptome Microarray','Y',NULL),
 (6,'Gene Expression Microarray','Y',NULL),
 (7,'Quantitative rtPCR','Y',NULL),
 (8,'SAGE','Y',NULL);
/*!40000 ALTER TABLE `ExperimentMethod` ENABLE KEYS */;


--
-- Definition of table `ExperimentPlatform`
--

DROP TABLE IF EXISTS `ExperimentPlatform`;
CREATE TABLE `ExperimentPlatform` (
  `idExperimentPlatform` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(100) NOT NULL,
  `isActive` char(1) default NULL,
  `idUser` int(10) unsigned default NULL,
  PRIMARY KEY  (`idExperimentPlatform`),
  KEY `FK_ExperimentPlatform_User` (`idUser`),
  CONSTRAINT `FK_ExperimentPlatform_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=8 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `ExperimentPlatform`
--

/*!40000 ALTER TABLE `ExperimentPlatform` DISABLE KEYS */;
INSERT INTO `ExperimentPlatform` (`idExperimentPlatform`,`name`,`isActive`,`idUser`) VALUES 
 (1,'Illumina Genome Analyzer','Y',NULL),
 (2,'Agilent Microarray','Y',NULL),
 (3,'Affymetrix Microarray','Y',NULL),
 (4,'454 Genome Sequencer','Y',NULL),
 (6,'Applied Biosystems SOLiD','Y',NULL),
 (7,'Helicos Genomic Signature Sequencing','Y',NULL);
/*!40000 ALTER TABLE `ExperimentPlatform` ENABLE KEYS */;


--
-- Definition of table `GenomeVersion`
--

DROP TABLE IF EXISTS `GenomeVersion`;
CREATE TABLE `GenomeVersion` (
  `idGenomeVersion` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(200) NOT NULL,
  `idOrganism` int(10) unsigned default NULL,
  `buildDate` datetime default NULL,
  `coordURI` varchar(2000) default NULL,
  `coordVersion` varchar(50) default NULL,
  `coordSource` varchar(50) default NULL,
  `coordTestRange` varchar(100) default NULL,
  `coordAuthority` varchar(50) default NULL,
  PRIMARY KEY  (`idGenomeVersion`),
  UNIQUE KEY `Index_GenomeVersionName` (`name`),
  KEY `FK_GenomeVersion_Organism` (`idOrganism`),
  CONSTRAINT `FK_GenomeVersion_Organism` FOREIGN KEY (`idOrganism`) REFERENCES `Organism` (`idOrganism`)
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `GenomeVersion`
--

/*!40000 ALTER TABLE `GenomeVersion` DISABLE KEYS */;
INSERT INTO `GenomeVersion` (`idGenomeVersion`,`name`,`idOrganism`,`buildDate`,`coordURI`,`coordVersion`,`coordSource`,`coordTestRange`,`coordAuthority`) VALUES 
 (400,'A_thaliana_Jan_2004',400,'2004-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (500,'B_taurus_Aug_2006',500,'2006-08-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (501,'B_taurus_Oct_2007',500,'2007-10-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1200,'C_elegans_May_2003',1200,'2003-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1201,'C_elegans_Mar_2004',1200,'2004-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1202,'C_elegans_Jan_2007',1200,'2007-01-01 00:00:00','http://www.wormbase.org/genome/C_elegans/WS180/','180','Chromosome','','WS'),
 (1203,'C_elegans_May_2007',1200,'2007-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1204,'C_elegans_May_2008',1200,'2008-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1500,'C_familiaris_May_2005',1500,'2005-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1900,'D_melanogaster_Apr_2004',1900,'2004-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1901,'D_melanogaster_Apr_2006',1900,'2006-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (1902,'D_melanogaster_Jan_2003',1900,'2003-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (2700,'D_rerio_Mar_2006',2700,'2006-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (2701,'D_rerio_Jul_2007',2700,'2007-07-01 00:00:00','http://zfin.org/genome/D_rerio/Zv7/','Zv7','Chromosome',NULL,'ZFISH_7'),
 (2900,'E_coli_Oct_2007',2900,'2007-10-01 00:00:00','','','','',''),
 (3100,'G_gallus_May_2006',3100,'2006-05-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3301,'G_max_Dec_2008',3300,'2008-12-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3400,'H_sapiens_Jun_2002',3400,'2002-06-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3401,'H_sapiens_Nov_2002',3400,'2002-11-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3402,'H_sapiens_Apr_2003',3400,'2003-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3403,'H_sapiens_Jul_2003',3400,'2003-07-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3404,'H_sapiens_May_2004',3400,'2004-05-01 00:00:00','http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B35.1/','35','Chromosome',NULL,'NCBI'),
 (3405,'H_sapiens_Mar_2006',3400,'2006-03-01 00:00:00','http://www.ncbi.nlm.nih.gov/genome/H_sapiens/B36.1/','36','Chromosome','','NCBI'),
 (3500,'M_mulatta_Jan_2006',3500,'2006-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3600,'M_truncatula_Aug_2007',3600,'2007-08-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3800,'M_musculus_Feb_2002',3800,'2002-02-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3801,'M_musculus_Feb_2003',3800,'2003-02-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3802,'M_musculus_Oct_2003',3800,'2003-10-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3803,'M_musculus_Aug_2005',3800,'2005-08-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3804,'M_musculus_Mar_2006',3800,'2006-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3806,'M_musculus_Jul_2007',3800,'2007-07-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (3900,'M_abscessus_Mar_2008',3900,'2008-03-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (4000,'M_smegmatis_Mar_2008',4000,'2008-03-01 00:00:00','','','','',''),
 (4100,'M_tuberculosis_Sep_2008',4100,'2008-09-01 00:00:00','','','','',''),
 (4300,'O_sativa_Jan_2007',4300,'2007-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (4301,'O_sativa_Jan_2009',4300,'2009-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (4302,'O_sativa_Jun_2009',4300,'2009-06-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (4800,'P_falciparum_Jul_2007',4800,'2007-07-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5000,'P_trichocarpa_Jun_2004',5000,'2004-06-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5200,'R_norvegicus_Jan_2003',5200,'2003-01-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5201,'R_norvegicus_Jun_2003',5200,'2003-06-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5202,'R_norvegicus_Nov_2004',5200,'2004-11-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5207,'S_cerevisiae_May_2008',5300,'2008-05-01 00:00:00','','','','',''),
 (5300,'S_cerevisiae_Oct_2003',5300,'2003-10-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5304,'S_cerevisiae_Feb_2006',5300,'2006-02-01 00:00:00','','','','',''),
 (5305,'S_cerevisiae_Jul_2007',5300,'2007-07-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5306,'S_cerevisiae_Apr_2008',5300,'2008-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5400,'S_pombe_Sep_2004',5400,'2004-09-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (5401,'S_pombe_Apr_2007',5400,'2007-04-01 00:00:00','http://www.sanger.ac.uk/Projects/S_pombe/Apr_2007','Apr_2007','Chromosome','','Sanger'),
 (5402,'S_pombe_Sep_2007',5400,'2007-09-01 00:00:00','','','','',''),
 (5500,'S_glossinidius_Jan_2006',5500,'2006-01-01 00:00:00','ftp://ftp.ncbi.nih.gov/genomes/Bacteria/Sodalis_glossinidius_morsitans/Jan_2006','Jan_2006','Chromosome',NULL,'NCBI'),
 (6000,'T_nigroviridis_Feb_2004',6000,'2004-02-01 00:00:00','','','','',''),
 (6100,'V_vinifera_Apr_2007',6100,'2007-04-01 00:00:00',NULL,NULL,NULL,NULL,NULL),
 (6201,'X_tropicalis_Aug_2005',6200,'2005-08-01 00:00:00','','','','','');
/*!40000 ALTER TABLE `GenomeVersion` ENABLE KEYS */;


--
-- Definition of table `GenomeVersionAlias`
--

DROP TABLE IF EXISTS `GenomeVersionAlias`;
CREATE TABLE `GenomeVersionAlias` (
  `idGenomeVersionAlias` int(10) unsigned NOT NULL auto_increment,
  `alias` varchar(100) NOT NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idGenomeVersionAlias`),
  KEY `FK_GenomeVersionAlias_GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_GenomeVersionAlias_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`)
) ENGINE=InnoDB AUTO_INCREMENT=10 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `GenomeVersionAlias`
--

/*!40000 ALTER TABLE `GenomeVersionAlias` DISABLE KEYS */;
INSERT INTO `GenomeVersionAlias` (`idGenomeVersionAlias`,`alias`,`idGenomeVersion`) VALUES 
 (1,'spApr07',6),
 (4,'sgJan06',7),
 (5,'danRer5',5),
 (6,'zv7',5),
 (7,'Zv7',5),
 (8,'hg17',9),
 (9,'hg18',1);
/*!40000 ALTER TABLE `GenomeVersionAlias` ENABLE KEYS */;


--
-- Definition of table `Organism`
--

DROP TABLE IF EXISTS `Organism`;
CREATE TABLE `Organism` (
  `idOrganism` int(10) unsigned NOT NULL auto_increment,
  `commonName` varchar(100) NOT NULL,
  `sortOrder` int(10) unsigned default NULL,
  `binomialName` varchar(200) NOT NULL,
  `NCBITaxID` varchar(45) default NULL,
  `idUser` int(10) unsigned default NULL,
  `name` varchar(200) NOT NULL,
  PRIMARY KEY  USING BTREE (`idOrganism`),
  UNIQUE KEY `Index_OrganismName` (`name`),
  UNIQUE KEY `Index_OrganismBinomialName` (`binomialName`)
) ENGINE=InnoDB AUTO_INCREMENT=6300 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Organism`
--

/*!40000 ALTER TABLE `Organism` DISABLE KEYS */;
INSERT INTO `Organism` (`idOrganism`,`commonName`,`sortOrder`,`binomialName`,`NCBITaxID`,`idUser`,`name`) VALUES 
 (100,'Lizard',NULL,'Anolis carolinensis',NULL,NULL,'A_carolinensis'),
 (200,'A.gambiae',NULL,'Anopheles gambiae',NULL,NULL,'A_gambiae'),
 (300,'A_mellifera',NULL,'Apis mellifera','',NULL,'A_mellifera'),
 (400,'A. thaliana',NULL,'Arabidopsis thaliana',NULL,NULL,'A_thaliana'),
 (500,'Cow',NULL,'Bos taurus',NULL,NULL,'B_taurus'),
 (600,'Lancet',NULL,'Branchiostoma floridae',NULL,NULL,'B_floridae'),
 (700,'Marmoset',NULL,'Callithrix jacchus',NULL,NULL,'C_jacchus'),
 (800,'Guinea Pig',NULL,'Cavia porcellus',NULL,NULL,'C_porcellus'),
 (900,'C. intestinalis',NULL,'Ciona intestinalis',NULL,NULL,'C_intestinalis'),
 (1000,'C. brenneri',NULL,'Caenorhabditis brenneri',NULL,NULL,'C_brenneri'),
 (1100,'C. briggsae',NULL,'Caenorhabditis briggsae',NULL,NULL,'C_briggsae'),
 (1200,'C. elegans',NULL,'Caenorhabditis elegans','6239',NULL,'C_elegans'),
 (1300,'C. japonica',NULL,'Caenorhabditis japonica',NULL,NULL,'C_japonica'),
 (1400,'C. remanei',NULL,'Caenorhabditis remanei',NULL,NULL,'C_remanei'),
 (1500,'Dog',NULL,'Canis lupus familiaris',NULL,NULL,'C_familiaris'),
 (1600,'D. ananassae',NULL,'Drosophila ananassae',NULL,NULL,'D_ananassae'),
 (1700,'D. erecta',NULL,'Drosophila erecta',NULL,NULL,'D_erecta'),
 (1800,'D. grimshawi',NULL,'Drosophila grimshawi',NULL,NULL,'D_grimshawi'),
 (1900,'D. melanogaster',NULL,'Drosophila melanogaster',NULL,NULL,'D_melanogaster'),
 (2000,'D. mojavensis',NULL,'Drosophila mojavensis',NULL,NULL,'D_mojavensis'),
 (2100,'D. persimilis',NULL,'Drosophila persimilis',NULL,NULL,'D_persimilis'),
 (2200,'D. pseudoobscura',NULL,'Drosophila pseudoobscura',NULL,NULL,'D_pseudoobscura'),
 (2300,'D. sechellia',NULL,'Drosophila sechellia',NULL,NULL,'D_sechellia'),
 (2400,'D. simulans',NULL,'Drosophila simulans',NULL,NULL,'D_simulans'),
 (2500,'D. virilis',NULL,'Drosophila virilis',NULL,NULL,'D_virilis'),
 (2600,'D. yakuba',NULL,'Drosophila yakuba',NULL,NULL,'D_yakuba'),
 (2700,'Zebrafish',NULL,'Danio rerio','7955',NULL,'D_rerio'),
 (2800,'Horse',NULL,'Equus caballus',NULL,NULL,'E_caballus'),
 (2900,'E. coli',NULL,'Escherichia coli',NULL,NULL,'E_coli'),
 (3000,'Cat',NULL,'Felis catus',NULL,NULL,'F_catus'),
 (3100,'Chicken',NULL,'Gallus gallus',NULL,NULL,'G_gallus'),
 (3200,'Stickleback G_aculeatus',NULL,'Gasterosteus aculeatus','',NULL,'G_aculeatus'),
 (3300,'Soybean',NULL,'Glycine max',NULL,NULL,'G_max'),
 (3400,'Human',NULL,'Homo sapiens','9606',NULL,'H_sapiens'),
 (3500,'Rhesus',NULL,'Macaca mulatta',NULL,NULL,'M_mulatta'),
 (3600,'Barrel Medic',NULL,'Medicago truncatula',NULL,NULL,'M_truncatula'),
 (3700,'Opossum',NULL,'Monodelphis domestica',NULL,NULL,'M_domestica'),
 (3800,'Mouse',NULL,'Mus musculus','10090',NULL,'M_musculus'),
 (3900,'M. abcessus',NULL,'Mycobacterium abscessus',NULL,NULL,'M_abscessus'),
 (4000,'M. smegmatis',NULL,'Mycobacterium smegmatis',NULL,NULL,'M_smegmatis'),
 (4100,'M. tuberculosis',NULL,'Mycobacterium tuberculosis',NULL,NULL,'M_tuberculosis'),
 (4200,'Platypus',NULL,'Ornithorhynchus anatinus',NULL,NULL,'O_anatinus'),
 (4300,'Rice',NULL,'Oryza sativa',NULL,NULL,'O_sativa'),
 (4400,'Medaka',NULL,'Oryzias latipes',NULL,NULL,'O_latipes'),
 (4500,'O. lucimarinus',NULL,'Ostreococcus lucimarinus',NULL,NULL,'O_lucimarinus_Apr_2007'),
 (4600,'Chimp',NULL,'Pan troglodytes',NULL,NULL,'P_troglodytes'),
 (4700,'Lamprey',NULL,'Petromyzon marinus',NULL,NULL,'P_marinus'),
 (4800,'P. falciparum',NULL,'Plasmodium falciparum',NULL,NULL,'P_falciparum'),
 (4900,'Orangutan',NULL,'Pongo pygmaeus abelii',NULL,NULL,'P_abelii'),
 (5000,'Black Cottonwood',NULL,'Populus trichocarpa',NULL,NULL,'P_trichocarpa'),
 (5100,'P. pacificus',NULL,'Pristionchus pacificus',NULL,NULL,'P_pacificus'),
 (5200,'Rat',NULL,'Rattus norvegicus',NULL,NULL,'R_norvegicus'),
 (5300,'Yeast',NULL,'Saccharomyces cerevisiae',NULL,NULL,'S_cerevisiae'),
 (5400,'Fission Yeast',NULL,'Schizosaccharomyces pombe','4896',NULL,'S_pombe'),
 (5500,'S. glossinidius',NULL,'Sodalis glossinidius','343509',NULL,'S_glossinidius'),
 (5600,'Sorghum',NULL,'Sorghum bicolor','',NULL,'S_bicolor'),
 (5700,'Purple Sea Urchin',NULL,'Strongylocentrotus purpuratus',NULL,NULL,'S_purpuratus'),
 (5800,'Zebra Finch',NULL,'Taeniopygia guttata','',NULL,'T_guttata'),
 (5900,'Fugu',NULL,'Takifugu rubripes','',NULL,'T_rubripes'),
 (6000,'Tetraodon',NULL,'Tetraodon nigroviridis','',NULL,'T_nigroviridis'),
 (6100,'Common Grape Vine',NULL,'Vitis vinifera',NULL,NULL,'V_vinifera'),
 (6200,'Pipid Frog',NULL,'Xenopus tropicalis','',NULL,'X_tropicalis');
/*!40000 ALTER TABLE `Organism` ENABLE KEYS */;



--
-- Definition of table `UnloadAnnotation`
--
DROP TABLE IF EXISTS `UnloadAnnotation`;
CREATE TABLE  `UnloadAnnotation` (
  `idUnloadAnnotation` int(10) unsigned NOT NULL auto_increment,
  `typeName` varchar(2000) NOT NULL,
  `idUser` int(10) unsigned default NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idUnloadAnnotation`),
  KEY `FK_UnloadAnnotation_User` (`idUser`),
  KEY `FK_UnloadAnnotation_GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_UnloadAnnotation_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_UnloadAnnotation_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`)
) ENGINE=InnoDB AUTO_INCREMENT=1 DEFAULT CHARSET=latin1;

--
-- Definition of table `UserGroup`
--

DROP TABLE IF EXISTS `UserGroup`;
CREATE TABLE `UserGroup` (
  `idUserGroup` int(10) unsigned NOT NULL auto_increment,
  `name` varchar(200) NOT NULL,
  `contact` varchar(500) default NULL,
  `email` varchar(500) default NULL,
  `institute` varchar(200) default NULL,  
  PRIMARY KEY  (`idUserGroup`)
) ENGINE=InnoDB AUTO_INCREMENT=7 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserGroup`
--

/*!40000 ALTER TABLE `UserGroup` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroup` ENABLE KEYS */;


--
-- Definition of table `UserGroupCollaborator`
--

DROP TABLE IF EXISTS `UserGroupCollaborator`;
CREATE TABLE `UserGroupCollaborator` (
  `idUserGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idUserGroup`,`idUser`),
  KEY `FK_UserGroupCollaborator_User` (`idUser`),
  CONSTRAINT `FK_UserGroupCollaborator_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_UserGroupCollaborator_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserGroupCollaborator`
--

/*!40000 ALTER TABLE `UserGroupCollaborator` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroupCollaborator` ENABLE KEYS */;


--
-- Definition of table `UserGroupManager`
--

DROP TABLE IF EXISTS `UserGroupManager`;
CREATE TABLE `UserGroupManager` (
  `idUserGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL auto_increment,
  PRIMARY KEY  (`idUserGroup`,`idUser`),
  KEY `FK_UserGroupManager_User` (`idUser`),
  CONSTRAINT `FK_UserGroupManager_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_UserGroupManager_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`)
) ENGINE=InnoDB AUTO_INCREMENT=25 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserGroupManager`
--

/*!40000 ALTER TABLE `UserGroupManager` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroupManager` ENABLE KEYS */;


--
-- Definition of table `UserGroupMember`
--

DROP TABLE IF EXISTS `UserGroupMember`;
CREATE TABLE `UserGroupMember` (
  `idUserGroup` int(10) unsigned NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  PRIMARY KEY  USING BTREE (`idUser`,`idUserGroup`),
  KEY `FK_UserGroupUser_UserGroup` (`idUserGroup`),
  CONSTRAINT `FK_UserGroupMember_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_UserGroupMember_UserGroup` FOREIGN KEY (`idUserGroup`) REFERENCES `UserGroup` (`idUserGroup`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1 PACK_KEYS=1;

--
-- Dumping data for table `UserGroupMember`
--

/*!40000 ALTER TABLE `UserGroupMember` DISABLE KEYS */;
/*!40000 ALTER TABLE `UserGroupMember` ENABLE KEYS */;


--
-- Definition of table `Segment`
--

DROP TABLE IF EXISTS `Segment`;
CREATE TABLE `Segment` (
  `idSegment` int(10) unsigned NOT NULL auto_increment,
  `length` int(10) unsigned NOT NULL,
  `name` varchar(100) NOT NULL,
  `idGenomeVersion` int(10) unsigned NOT NULL,
  `sortOrder` int(10) unsigned NOT NULL,
  PRIMARY KEY  (`idSegment`),
  KEY `FK_Segment_GenomeVersion` (`idGenomeVersion`),
  CONSTRAINT `FK_Segment_GenomeVersion` FOREIGN KEY (`idGenomeVersion`) REFERENCES `GenomeVersion` (`idGenomeVersion`)
) ENGINE=InnoDB AUTO_INCREMENT=220 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Segment`
--

/*!40000 ALTER TABLE `Segment` DISABLE KEYS */;
INSERT INTO `Segment` (`idSegment`,`length`,`name`,`idGenomeVersion`,`sortOrder`) VALUES 
 (1,247249719,'chr1',3405,1),
 (2,242951149,'chr2',3405,2),
 (3,199501827,'chr3',3405,3),
 (4,191273063,'chr4',3405,4),
 (5,180857866,'chr5',3405,5),
 (6,170899992,'chr6',3405,6),
 (7,158821424,'chr7',3405,7),
 (8,146274826,'chr8',3405,8),
 (9,140273252,'chr9',3405,9),
 (10,135374737,'chr10',3405,10),
 (11,134452384,'chr11',3405,11),
 (12,132349534,'chr12',3405,12),
 (13,114142980,'chr13',3405,13),
 (14,106368585,'chr14',3405,14),
 (15,100338915,'chr15',3405,15),
 (16,88827254,'chr16',3405,16),
 (17,78774742,'chr17',3405,17),
 (18,76117153,'chr18',3405,18),
 (19,63811651,'chr19',3405,19),
 (20,62435964,'chr20',3405,20),
 (21,46944323,'chr21',3405,21),
 (22,49691432,'chr22',3405,22),
 (23,154913754,'chrX',3405,23),
 (24,57772954,'chrY',3405,24),
 (25,16571,'chrM',3405,25),
 (26,56204684,'chr1',2701,1),
 (27,54366722,'chr2',2701,2),
 (28,62931207,'chr3',2701,3),
 (29,42602441,'chr4',2701,4),
 (30,70371393,'chr5',2701,5),
 (31,59200669,'chr6',2701,6),
 (32,70262009,'chr7',2701,7),
 (33,56456705,'chr8',2701,8),
 (34,51490918,'chr9',2701,9),
 (35,42379582,'chr10',2701,10),
 (36,44616367,'chr11',2701,11),
 (37,47523734,'chr12',2701,12),
 (38,53547397,'chr13',2701,13),
 (39,56522864,'chr14',2701,14),
 (40,46629432,'chr15',2701,15),
 (41,53070661,'chr16',2701,16),
 (42,52310423,'chr17',2701,17),
 (43,49281368,'chr18',2701,18),
 (45,46181231,'chr19',2701,19),
 (46,56528676,'chr20',2701,20),
 (47,46057314,'chr21',2701,21),
 (48,38981829,'chr22',2701,22),
 (49,46388020,'chr23',2701,23),
 (50,40293347,'chr24',2701,24),
 (51,32876240,'chr25',2701,25),
 (52,16596,'chrM',2701,26),
 (53,122532868,'chrNA',2701,27),
 (54,45965611,'chrScaffold',2701,28),
 (55,15072419,'chrI',5401,1),
 (56,15279316,'chrII',5401,2),
 (57,13783681,'chrIII',5401,3),
 (58,17493784,'chrIV',5401,4),
 (59,20919398,'chrV',5401,5),
 (60,13794,'chrM',5401,6),
 (61,17718852,'chrX',5401,7),
 (62,4171146,'chr',5500,1),
 (63,52166,'phiSG1',5500,2),
 (64,83307,'pSG1',5500,3),
 (65,27241,'pSG2',5500,4),
 (66,10811,'pSG3',5500,5),
 (67,5579133,'chr1',5401,1),
 (68,4539804,'chr2',5401,2),
 (69,2452883,'chr3',5401,3),
 (70,19431,'chrM',5401,4),
 (71,35236,'mat',5401,5),
 (188,247249719,'chr1',3404,1),
 (189,242951149,'chr2',3404,2),
 (190,199501827,'chr3',3404,3),
 (191,191273063,'chr4',3404,4),
 (192,180857866,'chr5',3404,5),
 (193,170899992,'chr6',3404,6),
 (194,158821424,'chr7',3404,7),
 (195,146274826,'chr8',3404,8),
 (196,140273252,'chr9',3404,9),
 (197,135374737,'chr10',3404,10),
 (198,134452384,'chr11',3404,11),
 (199,132349534,'chr12',3404,12),
 (200,114142980,'chr13',3404,13),
 (201,106368585,'chr14',3404,14),
 (202,100338915,'chr15',3404,15),
 (203,88827254,'chr16',3404,16),
 (204,78774742,'chr17',3404,17),
 (205,76117153,'chr18',3404,18),
 (206,63811651,'chr19',3404,19),
 (207,62435964,'chr20',3404,20),
 (208,46944323,'chr21',3404,21),
 (209,49691432,'chr22',3404,22),
 (210,154913754,'chrX',3404,23),
 (211,57772954,'chrY',3404,24),
 (212,16571,'chrM',3404,25),
 (213,15072421,'chrI',1204,1),
 (214,15279323,'chrII',1204,2),
 (215,13783681,'chrIII',1204,3),
 (216,17493785,'chrIV',1204,4),
 (217,20919568,'chrV',1204,5),
 (218,17718854,'chrX',1204,6),
 (219,13794,'chrM',1204,7);
/*!40000 ALTER TABLE `Segment` ENABLE KEYS */;


--
-- Definition of table `User`
--

DROP TABLE IF EXISTS `User`;
CREATE TABLE `User` (
  `idUser` int(10) unsigned NOT NULL auto_increment,
  `lastName` varchar(200) NOT NULL,
  `firstName` varchar(200) NOT NULL,
  `middleName` varchar(100) default NULL,
  `email` varchar(500) default NULL,
  `institute` varchar(200) default NULL,
  `UserName` varchar(30) default NULL,
  `password` varchar(200) default NULL,
  PRIMARY KEY  USING BTREE (`idUser`),
  UNIQUE KEY `Index_UserName` (`UserName`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `User`
--

/*!40000 ALTER TABLE `User` DISABLE KEYS */;
INSERT INTO `User` (`idUser`,`lastName`,`firstName`,`middleName`,`UserName`,`password`) VALUES 
 (1,'','guest','','guest','454326b776dc46d32bb1050efe72df5e'),
 (2,'','admin','','admin','a447a70b58594f44a798d54cb4081fc2');
/*!40000 ALTER TABLE `User` ENABLE KEYS */;


--
-- Definition of table `UserRole`
--

DROP TABLE IF EXISTS `UserRole`;
CREATE TABLE `UserRole` (
  `UserName` varchar(30) NOT NULL,
  `roleName` varchar(30) NOT NULL,
  `idUser` int(10) unsigned NOT NULL,
  `idUserRole` int(10) unsigned NOT NULL auto_increment,
  PRIMARY KEY  (`idUserRole`),
  KEY `FK_UserRole_Username` (`UserName`),
  KEY `FK_UserRole_User` (`idUser`),
  CONSTRAINT `FK_UserRole_User` FOREIGN KEY (`idUser`) REFERENCES `User` (`idUser`),
  CONSTRAINT `FK_UserRole_Username` FOREIGN KEY (`UserName`) REFERENCES `User` (`UserName`)
) ENGINE=InnoDB AUTO_INCREMENT=3 DEFAULT CHARSET=latin1;

--
-- Dumping data for table `UserRole`
--

/*!40000 ALTER TABLE `UserRole` DISABLE KEYS */;
INSERT INTO `UserRole` (`UserName`,`roleName`,`idUser`,`idUserRole`) VALUES 
 ('guest','guest',1,1),
 ('admin','admin',2,2);
/*!40000 ALTER TABLE `UserRole` ENABLE KEYS */;


--
-- Definition of table `Visibility`
--

DROP TABLE IF EXISTS `Visibility`;
CREATE TABLE `Visibility` (
  `codeVisibility` varchar(10) NOT NULL default '',
  `name` varchar(100) NOT NULL,
  PRIMARY KEY  (`codeVisibility`)
) ENGINE=InnoDB DEFAULT CHARSET=latin1;

--
-- Dumping data for table `Visibility`
--

/*!40000 ALTER TABLE `Visibility` DISABLE KEYS */;
INSERT INTO `Visibility` (`codeVisibility`,`name`) VALUES 
 ('MEM','Members'),
 ('MEMCOL','Members and Collaborators'),
 ('PUBLIC','Public');
/*!40000 ALTER TABLE `Visibility` ENABLE KEYS */;




/*!40101 SET SQL_MODE=@OLD_SQL_MODE */;
/*!40014 SET FOREIGN_KEY_CHECKS=@OLD_FOREIGN_KEY_CHECKS */;
/*!40014 SET UNIQUE_CHECKS=@OLD_UNIQUE_CHECKS */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;
/*!40101 SET CHARACTER_SET_RESULTS=@OLD_CHARACTER_SET_RESULTS */;
/*!40101 SET COLLATION_CONNECTION=@OLD_COLLATION_CONNECTION */;
/*!40101 SET CHARACTER_SET_CLIENT=@OLD_CHARACTER_SET_CLIENT */;