package fr.irit.smac.calicoba.scenarios;

import java.io.IOException;
import java.util.Arrays;
import java.util.Locale;
import java.util.Map;

import fr.irit.smac.calicoba.mas.Calicoba;
import fr.irit.smac.calicoba.mas.agents.criticality.BaseCriticalityFunction;
import fr.irit.smac.calicoba.mas.model_attributes.IValueProviderSetter;
import fr.irit.smac.calicoba.mas.model_attributes.ReadableModelAttribute;
import fr.irit.smac.calicoba.mas.model_attributes.WritableModelAttribute;
import fr.irit.smac.util.CsvFileWriter;

public class SIRTest {
  public static void main(String[] args) throws IOException {
    final int initS = 997;
    final int initI = 3;
    final int initR = 0;
    final int total = initS + initI + initR;
    final double initB = 0.4;
    final double initG = 0.04;
    SIRModel model = new SIRModel(initS, initI, initR, initB, initG);

    Calicoba calicoba = new Calicoba(true,
        String.format(Locale.ENGLISH, "SIR_%d_%d_%d_%f_%f", initS, initI, initR, initB, initG), false, 0, false);
    calicoba.addParameter(new WritableModelAttribute<>(new IValueProviderSetter<Double>() {
      @Override
      public Double get() {
        return model.getInfectionProbability();
      }

      @Override
      public void set(Double value) {
        model.setInfectionProbability(value);
      }
    }, "β", 0, 1));
    calicoba.addParameter(new WritableModelAttribute<>(new IValueProviderSetter<Double>() {
      @Override
      public Double get() {
        return model.getRecoveryProbability();
      }

      @Override
      public void set(Double value) {
        model.setRecoveryProbability(value);
      }
    }, "γ", 0, 1));
    calicoba.addOutput(new ReadableModelAttribute<>(model::getSusceptible, "S", 0, total));
    calicoba.addOutput(new ReadableModelAttribute<>(model::getInfected, "I", 0, total));
    calicoba.addOutput(new ReadableModelAttribute<>(model::getRecovered, "R", 0, total));
    calicoba.addObjective("obj1", new BaseCriticalityFunction(Arrays.asList("I")) {
      @Override
      protected double getImpl(final Map<String, Double> parameterValues) {
        double i = parameterValues.get("I");
        if (i <= 300) {
          return 0;
        } else {
          return i - 300;
        }
      }
    });
    calicoba.setInfluenceFunction((pName, pValue, objName, objCrit) -> {
      if (objName.equals("obj1")) {
        if (pName.equals("β")) {
          return model.getInfected() < 300 ? 0.0 : 1.0;
        }
      }
      return 0.0;
    });

    CsvFileWriter fw = new CsvFileWriter(calicoba.dumpDirectory() + "SIR.csv", false, true, "cycle", "S", "I", "R", "β",
        "γ");
    calicoba.setup();
    for (int i = 0; i < 100; i++) {
      fw.writeLine("" + i, model.getSusceptible(), model.getInfected(), model.getRecovered(),
          model.getInfectionProbability(), model.getRecoveryProbability());
      calicoba.step();
      model.step();
    }
    fw.close();
  }

  static class SIRModel {
    private double susceptible;
    private double infected;
    private double recovered;
    private double infectionProbability;
    private double recoveryProbability;

    public SIRModel(double susceptible, double infected, double recovered, double infectionProbability,
        double recoveryProbability) {
      this.susceptible = susceptible;
      this.infected = infected;
      this.recovered = recovered;
      this.infectionProbability = infectionProbability;
      this.recoveryProbability = recoveryProbability;
    }

    private double getTotalPopulation() {
      return this.susceptible + this.infected + this.recovered;
    }

    public void step() {
      double a = (this.infectionProbability * this.susceptible * this.infected) / this.getTotalPopulation();
      double b = this.recoveryProbability * this.infected;
      this.susceptible -= a;
      this.infected += a - b;
      this.recovered += b;
    }

    public double getSusceptible() {
      return this.susceptible;
    }

    public double getInfected() {
      return this.infected;
    }

    public double getRecovered() {
      return this.recovered;
    }

    public double getInfectionProbability() {
      return this.infectionProbability;
    }

    public void setInfectionProbability(double infectionProbability) {
      this.infectionProbability = infectionProbability;
    }

    public double getRecoveryProbability() {
      return this.recoveryProbability;
    }

    public void setRecoveryProbability(double recoveryProbability) {
      this.recoveryProbability = recoveryProbability;
    }
  }
}
