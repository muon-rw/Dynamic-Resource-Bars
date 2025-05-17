import sys
from PIL import Image

def reverse_sprite_sheet(input_path, output_path):
    # Define frame dimensions
    frame_width = 64
    frame_height = 16

    # Load the original sprite sheet
    original_image = Image.open(input_path)
    total_frames = original_image.height // frame_height

    # Create a new blank image with the same dimensions
    new_image = Image.new('RGBA', (original_image.width, original_image.height))

    # Iterate through the frames from bottom to top
    for i in range(total_frames):
        # Calculate the y-coordinate for the current frame
        y_coord = (total_frames - 1 - i) * frame_height
        # Crop the frame from the original image
        frame = original_image.crop((0, y_coord, frame_width, y_coord + frame_height))
        # Paste the frame into the new image
        new_image.paste(frame, (0, i * frame_height))

    # Save the new image
    new_image.save(output_path)

if __name__ == '__main__':
    if len(sys.argv) != 3:
        print("Usage: python reverse_sprite_sheet.py <input_path> <output_path>")
        sys.exit(1)
    reverse_sprite_sheet(sys.argv[1], sys.argv[2]) 