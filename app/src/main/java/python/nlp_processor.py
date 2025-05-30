import re
import json
from typing import Dict, Any

class NLPProcessor:
    def __init__(self, context=None):
        self.context = context
        self.patterns = {
            'mvola': {
                'depot': r'(?:reçu|crédit|dépôt).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)',
                'retrait': r'(?:envoyé|débit|retrait).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)',
                'transfert': r'(?:transfert|envoi).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)'
            },
            'airtel': {
                'depot': r'(?:received|crédit|reçu).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)',
                'retrait': r'(?:sent|débit|envoyé).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)',
                'transfert': r'(?:transfer|transfert).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)'
            },
            'orange': {
                'depot': r'(?:reçu|crédit|dépôt).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)',
                'retrait': r'(?:envoyé|débit|retrait).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)',
                'transfert': r'(?:transfert|envoi).*?(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA)'
            }
        }

    def analyze_sms(self, message: str, sender: str) -> Dict[str, Any]:
        """
        Analyse un SMS pour extraire les informations de transaction
        """
        try:
            # Normaliser le message
            message_lower = message.lower()

            # Déterminer le provider
            provider = self._get_provider(sender)

            # Extraire les informations
            transaction_type = self._extract_transaction_type(message_lower, provider)
            amount = self._extract_amount(message)
            phone_number = self._extract_phone_number(message)
            reference = self._extract_reference(message)

            # Vérifier si c'est une transaction valide
            is_valid = (
                    amount > 0 and
                    transaction_type != "AUTRE" and
                    provider != "unknown"
            )

            return {
                "is_valid": is_valid,
                "transaction_type": transaction_type,
                "amount": amount,
                "phone_number": phone_number,
                "reference": reference,
                "provider": provider,
                "confidence": self._calculate_confidence(message, transaction_type, amount)
            }

        except Exception as e:
            return {
                "is_valid": False,
                "transaction_type": "ERREUR",
                "amount": 0.0,
                "phone_number": "",
                "reference": "",
                "provider": "unknown",
                "confidence": 0.0,
                "error": str(e)
            }

    def _get_provider(self, sender: str) -> str:
        """Détermine le provider à partir de l'expéditeur"""
        sender_lower = sender.lower()
        if any(keyword in sender_lower for keyword in ['mvola', 'telma']):
            return 'mvola'
        elif 'airtel' in sender_lower:
            return 'airtel'
        elif 'orange' in sender_lower:
            return 'orange'
        return 'unknown'

    def _extract_transaction_type(self, message: str, provider: str) -> str:
        """Extrait le type de transaction"""
        # Mots-clés pour chaque type de transaction
        depot_keywords = ['reçu', 'crédit', 'dépôt', 'received', 'deposit']
        retrait_keywords = ['envoyé', 'débit', 'retrait', 'sent', 'withdrawal']
        transfert_keywords = ['transfert', 'envoi', 'transfer', 'send']

        if any(keyword in message for keyword in depot_keywords):
            return "DEPOT"
        elif any(keyword in message for keyword in retrait_keywords):
            return "RETRAIT"
        elif any(keyword in message for keyword in transfert_keywords):
            return "TRANSFERT"
        else:
            return "AUTRE"

    def _extract_amount(self, message: str) -> float:
        """Extrait le montant de la transaction"""
        # Patterns pour les montants
        amount_patterns = [
            r'(\d+(?:[.,]\d+)?)\s*(?:Ar|MGA|ariary)',
            r'(\d+(?:[.,]\d+)?)\s*(?:ar|mga)',
            r'(?:montant|amount)[\s:]*(\d+(?:[.,]\d+)?)',
            r'(\d+(?:[.,]\d+)?)\s*(?:francs?|fr)'
        ]

        for pattern in amount_patterns:
            match = re.search(pattern, message, re.IGNORECASE)
            if match:
                amount_str = match.group(1).replace(',', '.')
                try:
                    return float(amount_str)
                except ValueError:
                    continue

        return 0.0

    def _extract_phone_number(self, message: str) -> str:
        """Extrait le numéro de téléphone"""
        phone_patterns = [
            r'(?:\+261|261|0)?[23][2-9]\d{7}',
            r'(?:de|from|to|vers)[\s:]*(\+?261[23][2-9]\d{7})',
            r'(?:de|from|to|vers)[\s:]*(0[23][2-9]\d{7})'
        ]

        for pattern in phone_patterns:
            match = re.search(pattern, message)
            if match:
                return match.group(1) if match.lastindex else match.group(0)

        return ""

    def _extract_reference(self, message: str) -> str:
        """Extrait la référence de transaction"""
        ref_patterns = [
            r'(?:ref|référence|transaction|id)[\s:]*([A-Z0-9]+)',
            r'(?:code|ref)[\s:]*([A-Z0-9]{6,})',
            r'([A-Z]{2,}\d{4,})'
        ]

        for pattern in ref_patterns:
            match = re.search(pattern, message, re.IGNORECASE)
            if match:
                return match.group(1)

        return ""

    def _calculate_confidence(self, message: str, transaction_type: str, amount: float) -> float:
        """Calcule un score de confiance pour l'analyse"""
        confidence = 0.0

        # Base confidence si on a un type et un montant
        if transaction_type != "AUTRE" and amount > 0:
            confidence += 0.5

        # Bonus pour les mots-clés spécifiques
        mobile_money_keywords = ['mvola', 'airtel money', 'orange money', 'mobile money']
        if any(keyword in message.lower() for keyword in mobile_money_keywords):
            confidence += 0.3

        # Bonus pour la présence d'une référence
        if self._extract_reference(message):
            confidence += 0.2

        return min(confidence, 1.0)

# Instance globale pour l'utilisation depuis Android
processor = None

def initialize_processor(context=None):
    global processor
    processor = NLPProcessor(context)
    return processor

def analyze_sms(message: str, sender: str) -> Dict[str, Any]:
    global processor
    if processor is None:
        processor = NLPProcessor()
    return processor.analyze_sms(message, sender)
