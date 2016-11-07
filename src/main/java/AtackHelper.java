import model.*;


class AtackHelper {

    private static final int MAX_ATACK_RANGE = 600;

    private static AtackHelper helper;

    static synchronized AtackHelper getInstance() {
        if (helper == null) {
            helper = new AtackHelper();
        }
        return helper;
    }


    private Wizard self;
    private World world;
    private Game game;
    private Move move;


    /**
     * Сохраняем все входные данные в полях класса для упрощения доступа к ним.
     */
    void initializeTick(Wizard self, World world, Game game, Move move) {
        this.self = self;
        this.world = world;
        this.game = game;
        this.move = move;
    }


    /**
     * Находим ближайшую цель для атаки.
     */
    LivingUnit getNearestTarget() {
        LivingUnit preferableBuilding = null;
        LivingUnit preferableWizard = null;
        LivingUnit preferableMinions = null;

        for (LivingUnit livingUnit : world.getBuildings()) {
            if (livingUnit.getFaction() == Faction.NEUTRAL || livingUnit.getFaction() == self.getFaction()) {
                continue;
            }
            double distanceCurrent = self.getDistanceTo(livingUnit);
            if (distanceCurrent > MAX_ATACK_RANGE) {
                continue;
            }
            preferableBuilding = livingUnit;
        }
        for (LivingUnit livingUnit : world.getMinions()) {
            if (livingUnit.getFaction() == Faction.NEUTRAL || livingUnit.getFaction() == self.getFaction()) {
                continue;
            }
            double distanceCurrent = self.getDistanceTo(livingUnit);
            if (distanceCurrent > MAX_ATACK_RANGE) {
                continue;
            }
            if (preferableMinions == null) {
                preferableMinions = livingUnit;
            } else {
                int healthCurrent = livingUnit.getLife();
                double distanceNearest = self.getDistanceTo(preferableMinions);
                int healthNearest = preferableMinions.getLife();

                if ((healthNearest / (double) preferableMinions.getMaxLife()) * distanceNearest > (healthCurrent / (double) livingUnit.getMaxLife()) * distanceCurrent) {
                    preferableMinions = livingUnit;
                }
            }
        }
        for (LivingUnit livingUnit : world.getWizards()) {
            if (livingUnit.getFaction() == Faction.NEUTRAL || livingUnit.getFaction() == self.getFaction()) {
                continue;
            }
            double distanceCurrent = self.getDistanceTo(livingUnit);
            if (distanceCurrent > MAX_ATACK_RANGE) {
                continue;
            }
            if (preferableWizard == null) {
                preferableWizard = livingUnit;
            } else {
                int healthCurrent = livingUnit.getLife();
                double distanceNearest = self.getDistanceTo(preferableWizard);
                int healthNearest = preferableWizard.getLife();

                if ((healthNearest / (double) preferableWizard.getMaxLife()) * distanceNearest > (healthCurrent / (double) livingUnit.getMaxLife()) * distanceCurrent) {
                    preferableWizard = livingUnit;
                }
            }
        }

        double buildingCoff = 0.0;
        double wizardCoff = 0.0;
        double minionCoff = 0.0;
        if (preferableBuilding != null) {
            buildingCoff = (preferableBuilding.getLife() / ((double) preferableBuilding.getMaxLife() * 1.5)) * self.getDistanceTo(preferableBuilding);
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

}
