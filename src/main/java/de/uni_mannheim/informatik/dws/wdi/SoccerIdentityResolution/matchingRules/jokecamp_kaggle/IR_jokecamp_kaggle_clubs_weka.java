package de.uni_mannheim.informatik.dws.wdi.SoccerIdentityResolution.matchingRules.jokecamp_kaggle;

import java.io.File;

import de.uni_mannheim.informatik.dws.wdi.SoccerIdentityResolution.ErrorAnalysisClubs;
import de.uni_mannheim.informatik.dws.wdi.SoccerIdentityResolution.model.Club;
import de.uni_mannheim.informatik.dws.wdi.SoccerIdentityResolution.model.ClubXMLReader;
import de.uni_mannheim.informatik.dws.wdi.SoccerIdentityResolution.blockers.ClubBlockerByLeague;
import de.uni_mannheim.informatik.dws.wdi.SoccerIdentityResolution.comparators.ClubNameComparatorLevenshtein;
import de.uni_mannheim.informatik.dws.winter.matching.MatchingEngine;
import de.uni_mannheim.informatik.dws.winter.matching.MatchingEvaluator;
import de.uni_mannheim.informatik.dws.winter.matching.algorithms.RuleLearner;
import de.uni_mannheim.informatik.dws.winter.matching.blockers.NoBlocker;
import de.uni_mannheim.informatik.dws.winter.matching.blockers.StandardRecordBlocker;
import de.uni_mannheim.informatik.dws.winter.matching.rules.WekaMatchingRule;
import de.uni_mannheim.informatik.dws.winter.model.Correspondence;
import de.uni_mannheim.informatik.dws.winter.model.HashedDataSet;
import de.uni_mannheim.informatik.dws.winter.model.MatchingGoldStandard;
import de.uni_mannheim.informatik.dws.winter.model.Performance;
import de.uni_mannheim.informatik.dws.winter.model.defaultmodel.Attribute;
import de.uni_mannheim.informatik.dws.winter.model.io.CSVCorrespondenceFormatter;
import de.uni_mannheim.informatik.dws.winter.processing.Processable;

public class IR_jokecamp_kaggle_clubs_weka
{
    public static void main( String[] args ) throws Exception
    {
        // loading data
        HashedDataSet<Club, Attribute> dataKaggle = new HashedDataSet<>();
        new ClubXMLReader().loadFromXML(new File("data/input/kaggle.xml"), "/clubs/club", dataKaggle);
        HashedDataSet<Club, Attribute> dataJokecamp = new HashedDataSet<>();
        new ClubXMLReader().loadFromXML(new File("data/input/jokecamp-others.xml"), "/clubs/club", dataJokecamp);

        System.out.println("Sample from kaggle: " + dataKaggle.getRandomRecord());
        System.out.println("Sample from jokecamp: " + dataJokecamp.getRandomRecord());

        // create a matching rule
        String options[] = new String[] { "" };
        String modelType = "SimpleLogistic";
        WekaMatchingRule<Club, Attribute> matchingRule = new WekaMatchingRule<>(0.6, modelType, options);
        
        // add comparators
        matchingRule.addComparator(new ClubNameComparatorLevenshtein(false));
        matchingRule.addComparator(new ClubNameComparatorLevenshtein(true));
        
        // load the training set
        MatchingGoldStandard gsTraining = new MatchingGoldStandard();
        gsTraining.loadFromCSVFile(new File("data/goldstandard/gs_jokecamp_kaggle_clubs.csv"));
        
        // train the matching rule's model
        RuleLearner<Club, Attribute> learner = new RuleLearner<>();
        learner.learnMatchingRule(dataKaggle, dataJokecamp, null, matchingRule, gsTraining);

        // create a blocker (blocking strategy)
        NoBlocker<Club, Attribute> blocker = new NoBlocker<>();
        
        // Initialize Matching Engine
        MatchingEngine<Club, Attribute> engine = new MatchingEngine<>();

        // Execute the matching
        Processable<Correspondence<Club, Attribute>> correspondences = engine.runIdentityResolution(
                dataKaggle, dataJokecamp, null, matchingRule,
               blocker);

        // write the correspondences to the output file
        new CSVCorrespondenceFormatter().writeCSV(new File("data/output/jokecamp_kaggle_clubs_correspondences.csv"), correspondences);

        // load the gold standard (test set)
        MatchingGoldStandard gsTest = new MatchingGoldStandard();
        gsTest.loadFromCSVFile(new File(
                "data/goldstandard/gs_jokecamp_kaggle_clubs.csv"));

        // evaluate your result
        MatchingEvaluator<Club, Attribute> evaluator = new MatchingEvaluator<Club, Attribute>(true);
        Performance perfTest = evaluator.evaluateMatching(correspondences.get(),
                gsTest);
        new ErrorAnalysisClubs().printFalsePositives(correspondences, gsTest);
        new ErrorAnalysisClubs().printFalseNegatives(dataKaggle, dataJokecamp, correspondences, gsTest);
        // print the evaluation result
        System.out.println("Jokecamp <-> Kaggle");
        System.out
                .println(String.format(
                        "Precision: %.4f\nRecall: %.4f\nF1: %.4f",
                        perfTest.getPrecision(), perfTest.getRecall(),
                        perfTest.getF1()));
    }
}
