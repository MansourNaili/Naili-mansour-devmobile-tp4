package com.example.currencyconverter

import android.graphics.Paint
import android.os.Bundle
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.example.currencyconverter.databinding.ActivityMainBinding
import java.util.Locale

/**
 * Convertisseur de devises : un seul écran.
 * Les taux sont fixes, exprimés pour 1 EUR (l'euro sert de devise « pivot »).
 */
class MainActivity : AppCompatActivity() {

    // View Binding : donne accès à toutes les vues de activity_main.xml (binding.tvMontant, ...)
    private lateinit var binding: ActivityMainBinding

    // Taux de change fournis par le TP : combien vaut 1 EUR dans chaque devise
    private val taux = mapOf(
        "EUR" to 1.0, "USD" to 1.17, "TND" to 3.40,
        "GBP" to 0.87, "JPY" to 172.50, "CAD" to 1.62
    )

    private companion object {
        const val MONTANT_INITIAL = "0"   // valeur affichée au départ et après la touche C
        const val LONGUEUR_MAX = 10       // le montant est limité à 10 caractères
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        appliquerMargesSysteme()

        // Souligne le montant et le résultat (il n'existe pas d'attribut XML pour cela)
        binding.tvMontant.paintFlags = binding.tvMontant.paintFlags or Paint.UNDERLINE_TEXT_FLAG
        binding.tvResultat.paintFlags = binding.tvResultat.paintFlags or Paint.UNDERLINE_TEXT_FLAG

        configurerSpinners()
        configurerClavier()

        binding.btnConvertir.setOnClickListener { lancerConversion() }
        binding.btnInverser.setOnClickListener { inverserDevises() }
    }

    // ------------------------------------------------------------------
    // 4. Remplissage des deux Spinner
    // ------------------------------------------------------------------
    private fun configurerSpinners() {
        // Les devises viennent du string-array "devises" de strings.xml
        val devises = resources.getStringArray(R.array.devises)

        // item_spinner_devise = ligne fermée (texte blanc) ; dropdown = liste ouverte (layout Android)
        val adaptateur = ArrayAdapter.createFromResource(this, R.array.devises, R.layout.item_spinner_devise)
        adaptateur.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)

        binding.spDeviseSource.adapter = adaptateur
        binding.spDeviseCible.adapter = adaptateur

        // Par défaut : source = USD, cible = EUR
        binding.spDeviseSource.setSelection(devises.indexOf("USD"))
        binding.spDeviseCible.setSelection(devises.indexOf("EUR"))
    }

    // ------------------------------------------------------------------
    // 5. Clavier numérique
    // ------------------------------------------------------------------
    private fun configurerClavier() {
        // Les touches 0 à 9 et « . » appellent toutes ajouterCaractere avec leur propre texte
        val touches = listOf(
            binding.btn0, binding.btn1, binding.btn2, binding.btn3, binding.btn4,
            binding.btn5, binding.btn6, binding.btn7, binding.btn8, binding.btn9,
            binding.btnPoint
        )
        for (touche in touches) {
            touche.setOnClickListener { ajouterCaractere(touche.text.toString()) }
        }
        binding.btnEffacer.setOnClickListener { effacer() }
    }

    /** Ajoute un caractère (chiffre ou point) à la fin du montant, en respectant les règles du TP. */
    private fun ajouterCaractere(c: String) {
        val actuel = binding.tvMontant.text.toString()

        // Règle : un seul point décimal, une deuxième pression sur « . » est ignorée
        if (c == "." && actuel.contains(".")) return

        // Règle : le montant est limité à 10 caractères
        if (actuel.length >= LONGUEUR_MAX) return

        // Le montant initial « 0 » est remplacé par le premier chiffre tapé
        // (mais « 0 » suivi de « . » donne bien « 0. »)
        binding.tvMontant.text =
            if (actuel == MONTANT_INITIAL && c != ".") c else actuel + c
    }

    /** Touche C : le montant repasse à 0 et le résultat est vidé. */
    private fun effacer() {
        binding.tvMontant.text = MONTANT_INITIAL
        binding.tvResultat.text = ""
    }

    // ------------------------------------------------------------------
    // 6. Conversion
    // ------------------------------------------------------------------

    /** Clic sur « Convertir » : lit le montant et les deux devises choisies. */
    private fun lancerConversion() {
        val montant = lireMontant()

        // Règle : si le montant vaut 0, on affiche un Toast d'erreur
        if (montant == 0.0) {
            Toast.makeText(this, R.string.toast_montant_vide, Toast.LENGTH_SHORT).show()
            return
        }

        val source = binding.spDeviseSource.selectedItem.toString()
        val cible = binding.spDeviseCible.selectedItem.toString()
        afficherResultat(montant, source, cible)
    }

    /** Convertit puis affiche le résultat arrondi à 2 décimales. */
    private fun afficherResultat(montant: Double, source: String, cible: String) {
        val resultat = convertir(montant, source, cible)
        // Locale.US : force le POINT décimal (avec une langue française on obtiendrait « 102,56 »)
        binding.tvResultat.text = String.format(Locale.US, "%.2f", resultat)
    }

    /**
     * Passe par l'euro comme devise pivot :
     *   montant en EUR = montant / taux(source)
     *   résultat       = montant en EUR x taux(cible)
     */
    private fun convertir(montant: Double, source: String, cible: String): Double {
        // Règle : si source = cible, le résultat est égal au montant (pas d'erreur)
        if (source == cible) return montant

        val montantEnEuro = montant / taux.getValue(source)
        return montantEnEuro * taux.getValue(cible)
    }

    /** Transforme le texte du montant en nombre ; « 0 » si le texte n'est pas un nombre valide. */
    private fun lireMontant(): Double =
        binding.tvMontant.text.toString().toDoubleOrNull() ?: 0.0

    // ------------------------------------------------------------------
    // 7. Inversion des devises
    // ------------------------------------------------------------------
    private fun inverserDevises() {
        val positionSource = binding.spDeviseSource.selectedItemPosition
        val positionCible = binding.spDeviseCible.selectedItemPosition

        // Devises AVANT l'échange (on les garde pour recalculer sans relire les Spinner)
        val ancienneSource = binding.spDeviseSource.selectedItem.toString()
        val ancienneCible = binding.spDeviseCible.selectedItem.toString()

        // On échange les sélections des deux Spinner
        binding.spDeviseSource.setSelection(positionCible)
        binding.spDeviseCible.setSelection(positionSource)

        // Si un résultat est déjà affiché, on relance la conversion avec les devises échangées
        val montant = lireMontant()
        if (binding.tvResultat.text.isNotEmpty() && montant != 0.0) {
            afficherResultat(montant, source = ancienneCible, cible = ancienneSource)
        }
    }

    // ------------------------------------------------------------------
    // Barres système (affichage bord à bord des versions récentes d'Android)
    // ------------------------------------------------------------------
    private fun appliquerMargesSysteme() {
        val paddingHautInitial = binding.zoneHaut.paddingTop
        val paddingBasInitial = binding.tableClavier.paddingBottom

        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { _, insets ->
            val barres = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.zoneHaut.setPadding(
                binding.zoneHaut.paddingLeft, paddingHautInitial + barres.top,
                binding.zoneHaut.paddingRight, binding.zoneHaut.paddingBottom
            )
            binding.tableClavier.setPadding(
                binding.tableClavier.paddingLeft, binding.tableClavier.paddingTop,
                binding.tableClavier.paddingRight, paddingBasInitial + barres.bottom
            )
            insets
        }
        ViewCompat.requestApplyInsets(binding.root)
    }
}