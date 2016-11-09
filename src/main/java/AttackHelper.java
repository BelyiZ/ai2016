import model.*;


class AttackHelper {

    private static AttackHelper helper;

    static synchronized AttackHelper getInstance() {
        if (helper == null) {
            helper = new AttackHelper();
        }
        return helper;
    }


    private Wizard self;
    private World world;
    private Game game;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    void initializeTick(Wizard self, World world, Game game) {
        this.self = self;
        this.world = world;
        this.game = game;
    }


    LivingUnit getNearestTarget(LivingUnit[] units) {
        LivingUnit result = null;
        for (LivingUnit livingUnit : units) {
            if (livingUnit.getFaction() == Faction.NEUTRAL || livingUnit.getFaction() == self.getFaction()) {
                continue;
            }
            double distanceCurrent = self.getDistanceTo(livingUnit);
            if (distanceCurrent > self.getCastRange() + 200) {
                continue;
            }
            if (result == null) {
                result = livingUnit;
            } else {
                int healthCurrent = livingUnit.getLife();
                double distanceNearest = self.getDistanceTo(result);
                int healthNearest = result.getLife();

                if ((healthNearest / (double) result.getMaxLife()) * distanceNearest > (healthCurrent / (double) livingUnit.getMaxLife()) * distanceCurrent) {
                    result = livingUnit;
                }
            }
        }
        return result;
    }

    /**
     * Находим ближайшую цель для атаки.
     */
    LivingUnit getNearestTarget() {
        LivingUnit preferableBuilding = getNearestTarget(world.getBuildings());
        LivingUnit preferableWizard = getNearestTarget(world.getWizards());
        LivingUnit preferableMinions = getNearestTarget(world.getMinions());

        double buildingCoff = 0.0;
        double wizardCoff = 0.0;
        double minionCoff = 0.0;
        if (preferableBuilding != null) {
            buildingCoff = (preferableBuilding.getLife() / ((double) preferableBuilding.getMaxLife() * 0.5)) * self.getDistanceTo(preferableBuilding);
        }
        if (preferableWizard != null) {
            wizardCoff = (preferableWizard.getLife() / ((double) preferableWizard.getMaxLife() * 1.5)) * self.getDistanceTo(preferableWizard);
        }
        if (preferableMinions != null) {
            minionCoff = (preferableMinions.getLife() / (double) preferableMinions.getMaxLife()) * self.getDistanceTo(preferableMinions);
        }

        LivingUnit nearestUnit = preferableBuilding;
        double currentCoff = buildingCoff;
        if (minionCoff != 0 && (nearestUnit == null || currentCoff > minionCoff)) {
            nearestUnit = preferableMinions;
            currentCoff = minionCoff;
        }
        if (wizardCoff != 0 && (nearestUnit == null || currentCoff > wizardCoff)) {
            nearestUnit = preferableWizard;
        }

        return nearestUnit;
    }

    Double getDistanceToEnemyBase() {
        for (Building building : world.getBuildings()) {
            if (BuildingType.FACTION_BASE.equals(building.getType()) && !building.getFaction().equals(self.getFaction())) {
                return self.getDistanceTo(building);
            }
        }
        return null;
    }

}
