# Lookitecture

Your **personal, on-the-go tourist guide**, powered by AI and offline capability.

## Overview

**Lookitecture** is an Android application that uses on-device AI to recognize landmarks from photos taken or uploaded by the user. Designed with travelers and architecture enthusiasts in mind, the app allows users to learn about the world around them — anytime, anywhere.

## Core Technology

- **MobileNet Neural Network**  
  At the heart of Lookitecture lies a fine-tuned **MobileNet** model trained on a curated subset of landmark images.  
  Due to time constraints during development (24h hackathon), we trained the model on a limited dataset — but the results are promising and extensible.

- **Offline Capability**  
  Since the model is embedded directly in the app, users can identify landmarks **without an internet connection**, making it perfect for remote adventures or travel abroad.

- **GEMINI LLM Integration**  
  When online, Lookitecture queries **Google's Gemini LLM** to enrich landmark recognition with detailed descriptions and interesting facts.  
  When offline, a fallback to pre-downloaded landmark metadata ensures users still get informative results.

## Features

- **Scan Landmarks**  
  Take a photo or upload one from your gallery — Lookitecture will identify the landmark and tell you more about it.

- **Works Offline**  
  Landmark recognition is performed entirely on-device using a local neural network model.

- **AI-Powered Info**  
  Online users benefit from rich, real-time landmark information using the Gemini LLM.

- **View History**  
  All scanned landmarks are saved locally, allowing users to revisit past discoveries.

## Future Plans

- **Architectural Style Recognition**  
  An additional model to classify buildings by style (e.g., Baroque, Post-modernism, Gothic, etc.).

- **Enhanced Dataset**  
  Expanding our landmark dataset for better accuracy and wider global coverage.

- **Travel Guide**  
  A “travel guide” feature to show what landmarks other users visit.

## Tech Stack

- **Android (Jetpack Compose)**  
- **TensorFlow Lite (on-device MobileNet)**  
- **Google Gemini LLM API**  
- **Kotlin**  
- **Material3 UI**  

## Screenshots

*Coming soon!*

## Acknowledgements

- [TensorFlow Lite](https://www.tensorflow.org/lite)
- [MobileNet](https://github.com/tensorflow/models/tree/master/research/slim/nets/mobilenet)
- [Gemini LLM](https://deepmind.google/technologies/gemini/)
- The wonderful open-source libraries and APIs we relied on to build Lookitecture 💙

