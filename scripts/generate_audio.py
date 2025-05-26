import pyttsx3
import os

def generate_audio(text, filename):
    engine = pyttsx3.init()
    engine.setProperty('rate', 150)
    engine.save_to_file(text, filename)
    engine.runAndWait()

# Create audio directory if it doesn't exist
audio_dir = '../app/src/main/assets/audio'
os.makedirs(audio_dir, exist_ok=True)

# Generate error audio
error_text = "Unable to determine product quality with confidence. Please try again with better lighting and positioning."
generate_audio(error_text, os.path.join(audio_dir, 'error.mp3'))

# Generate audio for each product state
products = ['Apple', 'Banana', 'Mango', 'Orange', 'Strawberry', 
           'Carrot', 'Potato', 'Tomato', 'Cucumber', 'Bellpepper']

for product in products:
    # Fresh product
    fresh_text = f"This {product} is fresh and of excellent quality"
    generate_audio(fresh_text, os.path.join(audio_dir, f'Fresh{product}.mp3'))
    
    # Rotten product
    rotten_text = f"This {product} is rotten. Please choose another one"
    generate_audio(rotten_text, os.path.join(audio_dir, f'Rotten{product}.mp3'))

print("Audio files generated successfully!")
