/* E.V-MARK-X feminine voice helper.
 * Browser-only demo: no audio is uploaded and no AI answer is generated.
 * Include this file in a page and call EVVoice.speak("Hello") or EVVoice.listen().
 */
(() => {
  const chooseVoice = () => {
    if (!("speechSynthesis" in window)) return null;
    const voices = window.speechSynthesis.getVoices();
    const english = voices.filter(v => /^en(-|_)/i.test(v.lang));
    // Browser APIs don't expose a reliable gender field; these are heuristic voice-name hints.
    const feminineHints = /female|woman|samantha|victoria|karen|aria|jenny|zira|susan|google uk english female/i;
    return english.find(v => feminineHints.test(v.name)) || english.find(v => /en-US|en-GB/i.test(v.lang)) || voices[0] || null;
  };

  const speak = (text, options = {}) => {
    if (!("speechSynthesis" in window)) {
      return { ok: false, message: "Speech synthesis is not supported in this browser." };
    }
    window.speechSynthesis.cancel();
    const utterance = new SpeechSynthesisUtterance(String(text ?? ""));
    const voice = chooseVoice();
    if (voice) utterance.voice = voice;
    utterance.rate = options.rate ?? 1;
    utterance.pitch = options.pitch ?? 1.08;
    utterance.volume = options.volume ?? 1;
    window.speechSynthesis.speak(utterance);
    return { ok: true, voice: voice?.name ?? "browser default" };
  };

  const listen = (options = {}) => {
    const Recognition = window.SpeechRecognition || window.webkitSpeechRecognition;
    if (!Recognition) {
      return Promise.reject(new Error("Speech recognition is not supported in this browser."));
    }
    return new Promise((resolve, reject) => {
      const recognition = new Recognition();
      recognition.lang = options.lang || "en-US";
      recognition.interimResults = false;
      recognition.maxAlternatives = 1;
      recognition.onresult = event => resolve(event.results[0][0].transcript);
      recognition.onerror = event => reject(new Error(event.error || "Speech recognition failed."));
      recognition.start();
    });
  };

  window.EVVoice = { speak, listen, cancel: () => window.speechSynthesis?.cancel() };
  if ("speechSynthesis" in window) {
    window.speechSynthesis.onvoiceschanged = () => chooseVoice();
  }
})();
