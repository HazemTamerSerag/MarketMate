import torch
import torch.nn as nn
import torchvision.models as models
import sys
import os

def convert_pytorch_to_onnx(model_path, output_path, num_classes=20):
    """
    Convert a PyTorch model (.pth) to ONNX format.
    
    Args:
        model_path: Path to the PyTorch model file (.pth)
        output_path: Path where the ONNX model will be saved
        num_classes: Number of output classes (default: 20 for your fresh/rotten fruits/vegetables)
    """
    print(f"Loading PyTorch model from: {model_path}")
    
    # Load the model architecture - ResNet18 based on your code
    model = models.resnet18(pretrained=False)
    
    # Modify the final layer to match your number of classes
    model.fc = nn.Linear(model.fc.in_features, num_classes)
    
    # Load the trained weights
    try:
        model.load_state_dict(torch.load(model_path, map_location=torch.device('cpu')))
        print("Model weights loaded successfully")
    except Exception as e:
        print(f"Error loading model weights: {e}")
        return
    
    # Set the model to evaluation mode
    model.eval()
    
    # Create a dummy input tensor (1, 3, 224, 224) - batch_size, channels, height, width
    dummy_input = torch.randn(1, 3, 224, 224)
    
    # Export the model to ONNX format
    try:
        torch.onnx.export(
            model,               # PyTorch model
            dummy_input,         # Input tensor
            output_path,         # Output file path
            export_params=True,  # Store the trained weights
            opset_version=12,    # ONNX version
            do_constant_folding=True,  # Optimize model
            input_names=['input'],  # Model input name
            output_names=['output'],  # Model output name
            dynamic_axes={
                'input': {0: 'batch_size'},   # Variable batch size
                'output': {0: 'batch_size'}
            }
        )
        print(f"Model successfully converted to ONNX format: {output_path}")
        
        # Verify model size
        model_size = os.path.getsize(output_path) / (1024 * 1024)  # Convert to MB
        print(f"ONNX model size: {model_size:.2f} MB")
    except Exception as e:
        print(f"Error during ONNX conversion: {e}")

if __name__ == "__main__":
    if len(sys.argv) < 3:
        print("Usage: python convert_to_onnx.py <path_to_pth_model> <output_onnx_path> [num_classes]")
        sys.exit(1)
    
    model_path = sys.argv[1]
    output_path = sys.argv[2]
    num_classes = int(sys.argv[3]) if len(sys.argv) > 3 else 20
    
    convert_pytorch_to_onnx(model_path, output_path, num_classes)
