package dnd.engine.sheet;

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

/**
 * Un don d'origines et ses sous-choix : liste de sorts et caractéristique d'incantation d'Initié à la magie, compétences de
 * Doué. Seul le don est requis ; un sous-choix absent reste à choisir. Les sorts d'Initié à la magie restent à choisir : le
 * moteur ne connaît pas encore les sorts.
 */
public record OriginFeatChoice(OriginFeat feat, SpellList spellList, Ability spellcastingAbility, Set<Skill> skills) {

    public OriginFeatChoice {
        skills = skills == null || skills.isEmpty() ? Set.of() : Collections.unmodifiableSet(EnumSet.copyOf(skills));
    }

    /** Un don dont les sous-choix restent à faire. */
    public static OriginFeatChoice of(OriginFeat feat) {
        return new OriginFeatChoice(feat, null, null, Set.of());
    }

    /**
     * Refuse un choix impossible, avec la raison.
     *
     * @param taken les compétences déjà maîtrisées par ailleurs (classe, historique, espèce)
     */
    public void validate(Set<Skill> taken) {
        if (feat == null) throw new IllegalArgumentException("Choisir un don d’origines");
        String name = feat.label();
        if (feat != OriginFeat.MAGIC_INITIATE) {
            if (spellList != null) throw new IllegalArgumentException("%s : aucune liste de sorts à choisir".formatted(name));
            if (spellcastingAbility != null) {
                throw new IllegalArgumentException("%s : aucune caractéristique d’incantation à choisir".formatted(name));
            }
        } else if (spellcastingAbility != null && !SpeciesChoice.SPELLCASTING_ABILITIES.contains(spellcastingAbility)) {
            throw new IllegalArgumentException("Caractéristique d’incantation : Intelligence, Sagesse ou Charisme");
        }
        if (feat != OriginFeat.SKILLED) {
            if (!skills.isEmpty()) throw new IllegalArgumentException("%s : aucune compétence à choisir".formatted(name));
            return;
        }
        if (skills.size() > OriginFeat.SKILLED_COUNT) {
            throw new IllegalArgumentException("%s : %d compétences au plus".formatted(name, OriginFeat.SKILLED_COUNT));
        }
        for (Skill skill : skills) {
            if (taken.contains(skill)) throw new IllegalArgumentException("%s : %s déjà maîtrisée".formatted(name, skill.label()));
        }
    }

    /** Les compétences que le don rend maîtrisées. */
    public Set<Skill> grantedSkills() {
        return feat == OriginFeat.SKILLED ? skills : Set.of();
    }

    /** Le don tel qu'il s'écrit sur la fiche ; un sous-choix absent s'écrit « à choisir ». */
    public String text() {
        return switch (feat) {
            case SKILLED -> {
                List<String> chosen = Arrays.stream(Skill.values()).filter(skills::contains).map(Skill::label).toList();
                int missing = OriginFeat.SKILLED_COUNT - chosen.size();
                String list = String.join(", ", chosen);
                if (missing > 0) list += (list.isEmpty() ? "" : ", ") + missing + " à choisir";
                yield feat.label() + " : maîtrise de " + list;
            }
            case MAGIC_INITIATE -> "%s (%s) — incantation %s ; 2 sorts mineurs et 1 sort du 1er niveau (à choisir)".formatted(
                    feat.label(), spellList == null ? "liste à choisir" : spellList.label(),
                    spellcastingAbility == null ? "à choisir" : spellcastingAbility.label());
            case SAVAGE_ATTACKER, ALERT -> feat.label() + " — " + feat.text();
        };
    }
}
