import sys
import os
import torch
import torch.nn as nn
from torchvision import models

def convert_model():
    print("Starting model conversion process...")
    
    # Define input and output paths
    input_path = os.path.join("app", "src", "main", "assets", "best_model.pth")
    output_path = os.path.join("app", "src", "main", "assets", "best_model.onnx")
    
    # Check if input file exists
    if not os.path.exists(input_path):
        print(f"Error: Input file {input_path} not found")
        return False
    
    try:
        # Load model architecture (ResNet18)
        print("Loading ResNet18 model architecture...")
        model = models.resnet18(pretrained=False)
        
        # Modify final layer for 20 classes (fruits and vegetables, fresh and rotten)
        num_classes = 20
        model.fc = nn.Linear(model.fc.in_features, num_classes)
        print(f"Modified final layer for {num_classes} classes")
        
        # Load model weights
        print(f"Loading weights from {input_path}...")
        model.load_state_dict(torch.load(input_path, map_location=torch.device('cpu')))
        print("Weights loaded successfully")
        
        # Set model to evaluation mode
        model.eval()
        
        # Create dummy input (1, 3, 224, 224) - batch_size, channels, height, width
        dummy_input = torch.randn(1, 3, 224, 224)
        
        # Export to ONNX
        print(f"Exporting model to ONNX format: {output_path}...")
        torch.onnx.export(
            model,                  # PyTorch model
            dummy_input,            # Input tensor
            output_path,            # Output file path
            export_params=True,     # Store trained weights
            opset_version=11,       # ONNX version
            do_constant_folding=True,  # Optimize model
            input_names=['input'],  # Model input name
            output_names=['output'],  # Model output name
            dynamic_axes={
                'input': {0: 'batch_size'},   # Variable batch size
                'output': {0: 'batch_size'}
            }
        )
        
        # Verify file was created
        if os.path.exists(output_path):
            size_mb = os.path.getsize(output_path) / (1024 * 1024)
            print(f"ONNX model created successfully: {output_path} ({size_mb:.2f} MB)")
            return True
        else:
            print("Error: ONNX file was not created")
            return False
            
    except Exception as e:
        print(f"Error during conversion: {e}")
        return False

if __name__ == "__main__":
    success = convert_model()
    if success:
        print("Conversion completed successfully!")
    else:
        print("Conversion failed. See error messages above.")
