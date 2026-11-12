package dnd.engine.sheet;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * L'espèce d'une construction guidée et ses sous-choix : taille, option de trait (lignage, ascendance, héritage),
 * compétence, caractéristique d'incantation et don d'origines (Polyvalent de l'Humain). Seule l'espèce est requise ; un
 * sous-choix absent reste à choisir. Une espèce d'une seule taille la prend d'office.
 */
public record SpeciesChoice(Species species, CreatureSize size, SpeciesOption option, Skill skill,
                            Ability spellcastingAbility, OriginFeatChoice originFeat) {

    /** Les caractéristiques d'incantation qu'une espèce peut choisir. */
    public static final List<Ability> SPELLCASTING_ABILITIES = List.of(Ability.INTELLIGENCE, Ability.WISDOM, Ability.CHARISMA);

    public SpeciesChoice {
        if (size == null && species != null && species.sizes().size() == 1) size = species.sizes().getFirst();
    }

    /** Une espèce dont les sous-choix restent à faire. */
    public static SpeciesChoice of(Species species) {
        return new SpeciesChoice(species, null, null, null, null, null);
    }

    /** Cette espèce, avec ce don d'origines. */
    public SpeciesChoice withOriginFeat(OriginFeatChoice feat) {
        return new SpeciesChoice(species, size, option, skill, spellcastingAbility, feat);
    }

    /**
     * Refuse un choix impossible, avec la raison.
     *
     * @param classSkills      les compétences que la classe donne déjà
     * @param backgroundSkills les compétences que l'historique donne déjà
     */
    public void validate(Set<Skill> classSkills, Set<Skill> backgroundSkills) {
        if (species == null) throw new IllegalArgumentException("Choisir une espèce");
        String name = species.label();
        if (size != null && !species.sizes().contains(size)) {
            throw new IllegalArgumentException("%s : taille %s impossible".formatted(name, size.label()));
        }
        if (option != null && option.species() != species) {
            throw new IllegalArgumentException("%s : %s n’est pas une option de l’espèce".formatted(name, option.label()));
        }
        if (skill != null) {
            if (species.skillChoices().isEmpty()) {
                throw new IllegalArgumentException("%s : aucune compétence à choisir".formatted(name));
            }
            if (!species.skillChoices().contains(skill)) {
                throw new IllegalArgumentException("%s hors des compétences de %s".formatted(skill.label(), species.skillTrait()));
            }
            if (classSkills.contains(skill)) {
                throw new IllegalArgumentException("%s déjà maîtrisée par la classe".formatted(skill.label()));
            }
            if (backgroundSkills.contains(skill)) {
                throw new IllegalArgumentException("%s déjà donnée par l’historique".formatted(skill.label()));
            }
        }
        if (spellcastingAbility != null) {
            if (!species.spellcasting()) {
                throw new IllegalArgumentException("%s : aucune caractéristique d’incantation à choisir".formatted(name));
            }
            if (!SPELLCASTING_ABILITIES.contains(spellcastingAbility)) {
                throw new IllegalArgumentException("Caractéristique d’incantation : Intelligence, Sagesse ou Charisme");
            }
        }
        if (originFeat != null) {
            if (!species.choosesOriginFeat()) {
                throw new IllegalArgumentException("%s : aucun don d’origines à choisir".formatted(name));
            }
            Set<Skill> taken = EnumSet.noneOf(Skill.class);
            taken.addAll(classSkills);
            taken.addAll(backgroundSkills);
            if (skill != null) taken.add(skill);
            originFeat.validate(taken);
        }
    }

    /** Les compétences que l'espèce rend maîtrisées : celle de son trait et celles de son don. */
    public Set<Skill> grantedSkills() {
        Set<Skill> granted = EnumSet.noneOf(Skill.class);
        if (skill != null) granted.add(skill);
        if (originFeat != null) granted.addAll(originFeat.grantedSkills());
        return granted;
    }

    /** La taille telle qu'elle s'écrit sur la fiche. */
    public String sizeText() {
        if (size != null) return size.label();
        return String.join(" ou ", species.sizes().stream().map(s -> s.label().substring(0, 1)).toList()) + " (à choisir)";
    }

    /** La vitesse en mètres : celle de l'option si elle la change, sinon celle de l'espèce. */
    public double speed() {
        return option != null && option.speed() > 0 ? option.speed() : species.speed();
    }

    /** Les traits tels qu'ils s'écrivent sur la fiche, un par ligne ; un sous-choix absent s'écrit « à choisir ». */
    public String traitsText() {
        StringBuilder text = new StringBuilder();
        int darkvision = option != null && option.darkvision() > 0 ? option.darkvision() : species.darkvision();
        if (darkvision > 0) line(text, "Vision dans le noir : %d m".formatted(darkvision));
        species.traits().forEach(trait -> line(text, trait));
        if (species.optionLabel() != null) {
            line(text, species.optionLabel() + " : "
                    + (option == null ? "à choisir" : option.label() + " — " + option.text()));
        }
        if (species.spellcasting()) {
            line(text, "Caractéristique d’incantation : "
                    + (spellcastingAbility == null ? "à choisir" : spellcastingAbility.label()));
        }
        if (species.skillTrait() != null) {
            line(text, species.skillTrait() + " : "
                    + (skill == null ? "une compétence au choix (à choisir)" : skill.label() + " (maîtrise)"));
        }
        if (species.choosesOriginFeat()) {
            line(text, "Polyvalent : " + (originFeat == null ? "un don d’origines au choix (à choisir)" : originFeat.feat().label()));
        }
        return text.toString();
    }

    private static void line(StringBuilder text, String line) {
        if (!text.isEmpty()) text.append('\n');
        text.append(line);
    }
}
