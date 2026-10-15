package fr.doctorwho.tardis;

/** Étapes d'un voyage du TARDIS. */
public enum FlightPhase {
    /** Posé quelque part, portes utilisables. */
    LANDED,
    /** L'extérieur s'efface en clignotant. */
    DEMATERIALISING,
    /** Dans le vortex : plus d'extérieur nulle part. */
    IN_FLIGHT,
    /** L'extérieur apparaît à destination en clignotant. */
    MATERIALISING;

    public static FlightPhase byOrdinal(int ordinal) {
        FlightPhase[] phases = values();
        return ordinal >= 0 && ordinal < phases.length ? phases[ordinal] : LANDED;
    }
}
